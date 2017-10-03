package com.mikivan.service;


//[injections of mikivan][0001]
        import java.io.ByteArrayOutputStream;
//[end][0001]

        import java.io.BufferedOutputStream;
        import java.io.File;
        import java.io.IOException;
        import java.io.OutputStream;
        import java.security.GeneralSecurityException;
        import java.util.EnumSet;
        import java.util.List;
        import java.util.ResourceBundle;
        import java.util.concurrent.Executors;
        import java.util.concurrent.atomic.AtomicInteger;

        import javax.xml.transform.OutputKeys;
        import javax.xml.transform.Templates;
        import javax.xml.transform.TransformerFactory;
        import javax.xml.transform.sax.SAXTransformerFactory;
        import javax.xml.transform.sax.TransformerHandler;
        import javax.xml.transform.stream.StreamResult;
        import javax.xml.transform.stream.StreamSource;

        //import org.apache.commons.cli.CommandLine;
        //import org.apache.commons.cli.ParseException;
        import org.dcm4che3.data.*;
        import org.dcm4che3.io.DicomInputStream;
        import org.dcm4che3.io.DicomOutputStream;
        import org.dcm4che3.io.SAXReader;
        import org.dcm4che3.io.SAXWriter;
        import org.dcm4che3.net.ApplicationEntity;
        import org.dcm4che3.net.Association;
        import org.dcm4che3.net.Connection;
        import org.dcm4che3.net.Device;
        import org.dcm4che3.net.DimseRSPHandler;
        import org.dcm4che3.net.IncompatibleConnectionException;
        import org.dcm4che3.net.QueryOption;
        import org.dcm4che3.net.Status;
        import org.dcm4che3.net.pdu.AAssociateRQ;
        import org.dcm4che3.net.pdu.ExtendedNegotiation;
        import org.dcm4che3.net.pdu.PresentationContext;
        import org.dcm4che3.tool.common.CLIUtils;
        import org.dcm4che3.util.SafeClose;


public class CFindSCU {

    public static enum InformationModel {
        StudyRoot(UID.StudyRootQueryRetrieveInformationModelFIND, "STUDY");

        final String cuid;
        final String level;

        InformationModel(String cuid, String level) {
            this.cuid = cuid;
            this.level = level;
        }

        public void adjustQueryOptions(EnumSet<QueryOption> queryOptions) {
            if (level == null) {
                queryOptions.add(QueryOption.RELATIONAL);
                queryOptions.add(QueryOption.DATETIME);
            }
        }
    }

    private static String[] IVR_LE_FIRST = new String[]{"1.2.840.10008.1.2", "1.2.840.10008.1.2.1", "1.2.840.10008.1.2.2"};
    private static String[] EVR_LE_FIRST = new String[]{"1.2.840.10008.1.2.1", "1.2.840.10008.1.2.2", "1.2.840.10008.1.2"};
    private static String[] EVR_BE_FIRST = new String[]{"1.2.840.10008.1.2.2", "1.2.840.10008.1.2.1", "1.2.840.10008.1.2"};
    private static String[] IVR_LE_ONLY  = new String[]{"1.2.840.10008.1.2"};

    private static ResourceBundle rb =
            ResourceBundle.getBundle("org.dcm4che3.tool.findscu.messages");
    private static SAXTransformerFactory saxtf;

    private final Device device = new Device("findscu");
    private final ApplicationEntity ae = new ApplicationEntity("FINDSCU");
    private final Connection conn = new Connection();
    private final Connection remote = new Connection();
    private final AAssociateRQ rq = new AAssociateRQ();
    private int priority;
    private int cancelAfter;
    private InformationModel model;

    private int[] inFilter;
    private Attributes keys = new Attributes();

    //output
    private boolean catOut = true;               //"--out-cat" - объеденить все ответы от удаленного диком сервера
    //apply specified XSLT stylesheet to XML representation of received matches; implies -X
    private boolean xml = true;                  //"-X", "--xml" - вывод будет xml или обработаннный xslt.
    //use additional whitespace in XML output
    private boolean xmlIndent = true;           //"-I", "--indent" -
    //do not include keyword attribute of DicomAttribute element in XML output
    private boolean xmlIncludeKeyword = false;   //"-K", "--no-keyword" - включать ли keyword в xml вывод
    private boolean xmlIncludeNamespaceDeclaration = false;
    private File xsltFile;
    private Templates xsltTpls;
    private OutputStream out;

    private Association as;
    private AtomicInteger totNumMatches = new AtomicInteger();

    //[injections of mikivan][0002]
    private boolean isConstructorWithArgs = false;
    //[end][0002]


    public final void setPriority(int priority) {
        this.priority = priority;
    }

    public final void setInformationModel(
            InformationModel model,
            String[] tss,
            EnumSet<QueryOption> queryOptions) {

        this.model = model;
        rq.addPresentationContext(new PresentationContext(1, model.cuid, tss));

        if (!queryOptions.isEmpty()) {
            model.adjustQueryOptions(queryOptions);
            rq.addExtendedNegotiation(new ExtendedNegotiation(model.cuid,
                    QueryOption.toExtendedNegotiationInformation(queryOptions)));
        }

        if (model.level != null)
            addLevel(model.level);
    }

    public void addLevel(String s) {
        keys.setString(Tag.QueryRetrieveLevel, VR.CS, s);
    }

    public final void setCancelAfter(int cancelAfter) {
        this.cancelAfter = cancelAfter;
    }

    public final void setXSLT(File xsltFile) {
        this.xsltFile = xsltFile;
    }

    public final void setXML(boolean xml) { this.xml = xml; }

    public final void setXMLIndent(boolean indent) {
        this.xmlIndent = indent;
    }

    public final void setXMLIncludeKeyword(boolean includeKeyword) {
        this.xmlIncludeKeyword = includeKeyword;
    }

    public final void setXMLIncludeNamespaceDeclaration(
            boolean includeNamespaceDeclaration) {
        this.xmlIncludeNamespaceDeclaration = includeNamespaceDeclaration;
    }

    public final void setConcatenateOutputFiles(boolean catOut) {
        this.catOut = catOut;
    }

    public final void setInputFilter(int[] inFilter) {
        this.inFilter = inFilter;
    }

    public ApplicationEntity getApplicationEntity() {
        return ae;
    }

    public Connection getRemoteConnection() {
        return remote;
    }

    public AAssociateRQ getAAssociateRQ() {
        return rq;
    }

    public Association getAssociation() {
        return as;
    }

    public Device getDevice() {
        return device;
    }

    public Attributes getKeys() {
        return keys;
    }


    //------------------------------------------------------------------------------------------------------------------
    //------------------------------------------------------------------------------------------------------------------
    //------------------------------------------------------------------------------------------------------------------
     public CFindSCU(String[] b,
                    String[] c,
                    String[] opts,
                    String   fileXSLT,
                    String   findLevel,
                    String[] m,
                    String[] r)
            throws IOException {

        this.device.addConnection(conn);
        this.device.addApplicationEntity(ae);
        this.ae.addConnection(conn);

        //замена CLIUtils.configureConnect(this.remote, this.rq, cl);
        this.rq.setCalledAET(c[0]);
        this.remote.setHostname(c[1]);
        this.remote.setPort(Integer.parseInt(c[2]));
        //без настройки proxy, ест в CLIUtils.configureConnect

        //замена CLIUtils.configureBind(this.conn, this.ae, cl);
        this.ae.setAETitle(b[0]);
        this.conn.setHostname(b[1]);
        this.conn.setPort(Integer.parseInt(b[2]));


        //замена CLIUtils.configure(this.conn, cl);
        configure(this.conn, opts);

        this.remote.setTlsProtocols(this.conn.getTlsProtocols());
        this.remote.setTlsCipherSuites(this.conn.getTlsCipherSuites());

        //configureServiceClass(this, cl);
        this.setInformationModel(
                InformationModel.StudyRoot,
                IVR_LE_FIRST,//CLIUtils.transferSyntaxesOf(cl),
                queryOptionsOf(this));

        //configureKeys(this, cl);
        this.addLevel(findLevel);
        CLIUtils.addEmptyAttributes(this.keys, r);
        CLIUtils.addAttributes(this.keys, m);

        //configureOutput(this, cl);
        if (!fileXSLT.equals("")) this.setXSLT( new File( fileXSLT ) );

        //configureCancel(this, cl);

        //this.setPriority(CLIUtils.priorityOf(cl));
        this.setPriority(0);

        this.isConstructorWithArgs = true;
    }


    public String doFind() throws  Exception{
        if(this.isConstructorWithArgs){

            this.device.setExecutor2( Executors.newSingleThreadExecutor() );
            this.device.setScheduledExecutor( Executors.newSingleThreadScheduledExecutor() );

            this.open();

            this.query();

            if (this.as != null && this.as.isReadyForDataTransfer()) {
                this.as.waitForOutstandingRSP();
                this.as.release();
            }

            String output;
            //вывод
            if( this.out == null ){
                output = null;
            }
            else {
                output = this.out.toString();
            }

            SafeClose.close(this.out);
            this.out = null;

            this.device.getExecutor2().shutdown();
            this.device.getScheduledExecutor().shutdown();

            this.as.waitForSocketClose();

            this.isConstructorWithArgs = false;

            return output;
        }
        else{

            return null;
        }
    }
    //------------------------------------------------------------------------------------------------------------------
    //------------------------------------------------------------------------------------------------------------------
    //------------------------------------------------------------------------------------------------------------------


    @SuppressWarnings("unchecked")
    public static void main(String[] args) {
//======================================================================================================================
//-b IVAN@192.168.0.74:4006 -c PACS01@192.168.0.35:4006 -L STUDY -m StudyDate=20120101-20161231 -m ModalitiesInStudy=CT --out-cat  -X -K -I

        try {

            for (int i = 0; i < args.length ; i++ ) System.out.println( "args[" + i + "] = " + args[i] );

//            String[] bind   = { "IVAN",    "192.168.121.101", "4006"};//строгий порядок
//            String[] remote = { "WATCHER", "192.168.121.100", "4006"};//строгий порядок
            String[] bind   = { "IVAN",   "192.168.0.74", "4006"};//строгий порядок
            String[] remote = { "PACS01", "192.168.0.35", "4006"};//строгий порядок
            String[] opts   = {};
            String[] m      = { "StudyDate", "20121002-20171002", "ModalitiesInStudy", "CT"};
            String[] r      = {"0020000D", "00080020", "00080030", "00080050", "00080090", "00100010", "00100020",
                               "00100030", "00100040", "00200010", "00201206", "00201208", "00081030", "00080060",
                               "00080061"};


            System.out.println("------------------------------------------------------");
            CFindSCU main = new CFindSCU(bind, remote, opts, "xslt/mikivan-studies.xsl","STUDY", m, r);
            System.out.println("======================================================");
            String xsltOutput = main.doFind();


            //вывод будущего метода - сделать поиск - doFind();
            if( xsltOutput == null ){
                System.out.println("xmlOutput == null");
            }
            else {
                System.out.println(xsltOutput);
            }


        } catch (Exception e) {
            System.err.println("findscu: " + e.getMessage());
            e.printStackTrace();
            System.exit(2);
        }

    }
//======================================================================================================================

    private static void configure(Connection conn, String[] opts) throws IOException {
         //пока сделаем все по умолчанию, предполагая список опций в - opts
        //каждый ключ на своем строгом месте в массиве т.е. строгий порядок
        conn.setReceivePDULength(16378);  //"max-pdulen-rcv"
        conn.setSendPDULength(16378);     //"max-pdulen-snd"

        //используем не асинхронный режим if (cl.hasOption("not-async")) {
        conn.setMaxOpsInvoked(1);         //"max-ops-invoked"
        conn.setMaxOpsPerformed(1);       //"max-ops-performed"
//      } else {
//          conn.setMaxOpsInvoked(getIntOption(cl, "max-ops-invoked", 0));
//          conn.setMaxOpsPerformed(getIntOption(cl, "max-ops-performed", 0));
//      }

        conn.setPackPDV(false);           //"not-pack-pdv"
        conn.setConnectTimeout(0);        //"connect-timeout"
        conn.setRequestTimeout(0);        //"request-timeout"
        conn.setAcceptTimeout(0);         //"accept-timeout"
        conn.setReleaseTimeout(0);        //"release-timeout"
        conn.setResponseTimeout(0);       //"response-timeout"
        conn.setRetrieveTimeout(0);       //"retrieve-timeout"
        conn.setIdleTimeout(0);           //"idle-timeout"
        conn.setSocketCloseDelay(50);     //"soclose-delay"
        conn.setSendBufferSize(0);        //"sosnd-buffer"
        conn.setReceiveBufferSize(0);     //"sorcv-buffer"
        conn.setTcpNoDelay(false);        //"tcp-delay"

        // пока без применения TLS протокола
        //configureTLS(conn, cl);
    }


    private static EnumSet<QueryOption> queryOptionsOf(CFindSCU main) {
        EnumSet<QueryOption> queryOptions = EnumSet.noneOf(QueryOption.class);
        if(false) {
            queryOptions.add(QueryOption.RELATIONAL);  //"relational"
            queryOptions.add(QueryOption.DATETIME);    //"datetime"
            queryOptions.add(QueryOption.FUZZY);       //"fuzzy"
            queryOptions.add(QueryOption.TIMEZONE);    //"timezone"
        }
        return queryOptions;
    }


//    private static void configureCancel(CFindSCU main, CommandLine cl) {
//        if (cl.hasOption("cancel"))
//            main.setCancelAfter(Integer.parseInt(cl.getOptionValue("cancel")));
//    }

//    private static void configureKeys(CFindSCU main, CommandLine cl) {
//        CLIUtils.addEmptyAttributes(main.keys, cl.getOptionValues("r"));
//        CLIUtils.addAttributes(main.keys, cl.getOptionValues("m"));
////        if (cl.hasOption("L"))
////            main.addLevel(cl.getOptionValue("L"));
////        if (cl.hasOption("i"))
////            main.setInputFilter(CLIUtils.toTags(cl.getOptionValues("i")));
//    }


    public void open() throws IOException, InterruptedException,
            IncompatibleConnectionException, GeneralSecurityException {
        as = ae.connect(conn, remote, rq);
    }

    public void close() throws IOException, InterruptedException {
        if (as != null && as.isReadyForDataTransfer()) {
            as.waitForOutstandingRSP();
            as.release();
        }
        SafeClose.close(out);
        out = null;
    }


    private static class MergeNested implements Attributes.Visitor {
        private final Attributes keys;

        MergeNested(Attributes keys) {
            this.keys = keys;
        }

        @Override
        public boolean visit(Attributes attrs, int tag, VR vr, Object val) {
            if (isNotEmptySequence(val)) {
                Object o = keys.remove(tag);
                if (isNotEmptySequence(o))
                    ((Sequence) val).get(0).addAll(((Sequence) o).get(0));
            }
            return true;
        }

        private static boolean isNotEmptySequence(Object val) {
            return val instanceof Sequence && !((Sequence) val).isEmpty();
        }
    }

    static void mergeKeys(Attributes attrs, Attributes keys) {
        try {
            attrs.accept(new MergeNested(keys), false);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        attrs.addAll(keys);
    }

    public void query() throws IOException, InterruptedException {
        query(keys);
    }

    private void query(Attributes keys) throws IOException, InterruptedException {
        DimseRSPHandler rspHandler = new DimseRSPHandler(as.nextMessageID()) {

            int cancelAfter = CFindSCU.this.cancelAfter;
            int numMatches;

            @Override
            public void onDimseRSP(Association as, Attributes cmd,
                                   Attributes data) {
                super.onDimseRSP(as, cmd, data);
                int status = cmd.getInt(Tag.Status, -1);
                if (Status.isPending(status)) {
                    CFindSCU.this.onResult(data);
                    ++numMatches;
                    if (cancelAfter != 0 && numMatches >= cancelAfter)
                        try {
                            cancel(as);
                            cancelAfter = 0;
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                }
            }
        };

        query(keys, rspHandler);
    }

    public void query( DimseRSPHandler rspHandler) throws IOException, InterruptedException {
        query(keys, rspHandler);
    }

    private void query(Attributes keys, DimseRSPHandler rspHandler) throws IOException, InterruptedException {
        as.cfind(model.cuid, priority, keys, null, rspHandler);
    }

    private void onResult(Attributes data) {
        int numMatches = totNumMatches.incrementAndGet();

        //[injections of comment from mikivan][0006]

        //отключили проверку наличия ключа --out-dir
        /*
        if (outDir == null)
            return;
        */
        //[end][0006]

        try {
            if (out == null) {

                //[injections of comment from mikivan][0007]

                //здесь определяется тип выходного потока
                /*
                File f = new File(outDir, fname(numMatches));
                out = new BufferedOutputStream( new FileOutputStream(f) );
                */
                //[end][0007]

                //[injections of mikivan][0008]

                //собираем вывод в ByteArrayOutputStream - представляет поток вывода,
                //использующий массив байтов в качестве места вывода.

                out = new ByteArrayOutputStream();
                //[end][0008]

            }
            if (xml) {
                writeAsXML(data, out);
            } else {
                DicomOutputStream dos =
                        new DicomOutputStream(out, UID.ImplicitVRLittleEndian);
                dos.writeDataset(null, data);
            }
            out.flush();
        } catch (Exception e) {
            e.printStackTrace();
            SafeClose.close(out);
            out = null;
        } finally {
            if (!catOut) {
                SafeClose.close(out);
                out = null;
            }
        }
    }


    private void writeAsXML(Attributes attrs, OutputStream out) throws Exception {
        TransformerHandler th = getTransformerHandler();
        th.getTransformer().setOutputProperty(OutputKeys.INDENT,
                xmlIndent ? "yes" : "no");
        th.setResult(new StreamResult(out));
        SAXWriter saxWriter = new SAXWriter(th);
        saxWriter.setIncludeKeyword(xmlIncludeKeyword);
        saxWriter.setIncludeNamespaceDeclaration(xmlIncludeNamespaceDeclaration);
        saxWriter.write(attrs);
    }

    private TransformerHandler getTransformerHandler() throws Exception {
        SAXTransformerFactory tf = saxtf;
        if (tf == null)
            saxtf = tf = (SAXTransformerFactory) TransformerFactory
                    .newInstance();
        if (xsltFile == null)
            return tf.newTransformerHandler();

        Templates tpls = xsltTpls;
        if (tpls == null);
        xsltTpls = tpls = tf.newTemplates(new StreamSource(xsltFile));

        return tf.newTransformerHandler(tpls);
    }


}
