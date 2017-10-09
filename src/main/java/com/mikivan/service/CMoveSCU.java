package com.mikivan.service;

        //import java.io.File;
        import java.io.IOException;
        import java.security.GeneralSecurityException;
        //import java.text.MessageFormat;
        //import java.util.List;
        import java.util.ResourceBundle;
        //import java.util.concurrent.ExecutorService;
        import java.util.concurrent.Executors;
        //import java.util.concurrent.ScheduledExecutorService;

        //import org.apache.commons.cli.CommandLine;
        //import org.apache.commons.cli.OptionBuilder;
        //import org.apache.commons.cli.Options;
        import org.apache.commons.cli.ParseException;
        import org.dcm4che3.data.Tag;
        import org.dcm4che3.data.UID;
        import org.dcm4che3.data.Attributes;
        import org.dcm4che3.data.ElementDictionary;
        import org.dcm4che3.data.VR;
        import org.dcm4che3.io.DicomInputStream;
        import org.dcm4che3.net.ApplicationEntity;
        import org.dcm4che3.net.Association;
        import org.dcm4che3.net.Connection;
        import org.dcm4che3.net.Device;
        import org.dcm4che3.net.DimseRSPHandler;
        import org.dcm4che3.net.IncompatibleConnectionException;
        import org.dcm4che3.net.pdu.AAssociateRQ;
        import org.dcm4che3.net.pdu.ExtendedNegotiation;
        import org.dcm4che3.net.pdu.PresentationContext;
        import org.dcm4che3.tool.common.CLIUtils;
        //import org.dcm4che3.util.SafeClose;
        //import org.dcm4che3.util.StringUtils;

        //import org.dcm4che3.net.Dimse;


public class CMoveSCU extends Device {

    private static enum InformationModel {
        StudyRoot(UID.StudyRootQueryRetrieveInformationModelMOVE, "STUDY");

        final String cuid;
        final String level;

        InformationModel(String cuid, String level) {
            this.cuid = cuid;
            this.level = level;
        }
    }

    private static ResourceBundle rb =
            ResourceBundle.getBundle("org.dcm4che3.tool.movescu.messages");

    private static final int[] DEF_IN_FILTER = {
            Tag.SOPInstanceUID,
            Tag.StudyInstanceUID,
            Tag.SeriesInstanceUID
    };

    private final ApplicationEntity ae = new ApplicationEntity("movescu");
    private final Connection conn = new Connection();
    private final Connection remote = new Connection();
    private final AAssociateRQ rq = new AAssociateRQ();
    private int priority;
    private String destination;
    private InformationModel model;
    private Attributes keys = new Attributes();
    private int[] inFilter = DEF_IN_FILTER;
    private Association as;

    //[injections of mikivan][0002]
    private boolean isConstructorWithArgs = false;
    //[end][0002]
    private static String[] IVR_LE_FIRST = new String[]{"1.2.840.10008.1.2", "1.2.840.10008.1.2.1", "1.2.840.10008.1.2.2"};
    private static String[] EVR_LE_FIRST = new String[]{"1.2.840.10008.1.2.1", "1.2.840.10008.1.2.2", "1.2.840.10008.1.2"};
    private static String[] EVR_BE_FIRST = new String[]{"1.2.840.10008.1.2.2", "1.2.840.10008.1.2.1", "1.2.840.10008.1.2"};
    private static String[] IVR_LE_ONLY  = new String[]{"1.2.840.10008.1.2"};



//    public CMoveSCU() throws IOException {
//        super("movescu");
//        addConnection(conn);
//        addApplicationEntity(ae);
//        ae.addConnection(conn);
//    }

    public final void setPriority(int priority) {
        this.priority = priority;
    }

    public final void setInformationModel(InformationModel model, String[] tss,
                                          boolean relational) {
        this.model = model;
        rq.addPresentationContext(new PresentationContext(1, model.cuid, tss));
        if (relational)
            rq.addExtendedNegotiation(new ExtendedNegotiation(model.cuid, new byte[]{1}));
        if (model.level != null)
            addLevel(model.level);
    }

    public void addLevel(String s) {
        keys.setString(Tag.QueryRetrieveLevel, VR.CS, s);
    }

    public final void setDestination(String destination) {
        this.destination = destination;
    }

    public void addKey(int tag, String... ss) {
        VR vr = ElementDictionary.vrOf(tag, keys.getPrivateCreator(tag));
        keys.setString(tag, vr, ss);
    }

    public final void setInputFilter(int[] inFilter) {
        this.inFilter  = inFilter;
    }



//    private static CommandLine parseComandLine(String[] args)
//            throws ParseException {
//        Options opts = new Options();
//        addServiceClassOptions(opts);
//        addKeyOptions(opts);
//        addRetrieveLevelOption(opts);
//        addDestinationOption(opts);
//        CLIUtils.addConnectOption(opts);
//        CLIUtils.addBindOption(opts, "MOVESCU");
//        CLIUtils.addAEOptions(opts);
//        CLIUtils.addRetrieveTimeoutOption(opts);
//        CLIUtils.addPriorityOption(opts);
//        CLIUtils.addCommonOptions(opts);
//        return CLIUtils.parseComandLine(args, opts, rb, CMoveSCU.class);
//    }
//
//    @SuppressWarnings("static-access")
//    private static void addRetrieveLevelOption(Options opts) {
//        opts.addOption(OptionBuilder
//                .hasArg()
//                .withArgName("PATIENT|STUDY|SERIES|IMAGE|FRAME")
//                .withDescription(rb.getString("level"))
//                .create("L"));
//    }
//
//    @SuppressWarnings("static-access")
//    private static void addDestinationOption(Options opts) {
//        opts.addOption(OptionBuilder
//                .withLongOpt("dest")
//                .hasArg()
//                .withArgName("aet")
//                .withDescription(rb.getString("dest"))
//                .create());
//
//    }
//
//    @SuppressWarnings("static-access")
//    private static void addKeyOptions(Options opts) {
//        opts.addOption(OptionBuilder
//                .hasArgs()
//                .withArgName("attr=value")
//                .withValueSeparator('=')
//                .withDescription(rb.getString("match"))
//                .create("m"));
//        opts.addOption(OptionBuilder
//                .hasArgs()
//                .withArgName("attr")
//                .withDescription(rb.getString("in-attr"))
//                .create("i"));
//    }
//
//    @SuppressWarnings("static-access")
//    private static void addServiceClassOptions(Options opts) {
//        opts.addOption(OptionBuilder
//                .hasArg()
//                .withArgName("name")
//                .withDescription(rb.getString("model"))
//                .create("M"));
//        CLIUtils.addTransferSyntaxOptions(opts);
//        opts.addOption(null, "relational", false, rb.getString("relational"));
//    }

    //------------------------------------------------------------------------------------------------------------------
    //------------------------------------------------------------------------------------------------------------------
    //------------------------------------------------------------------------------------------------------------------
    //[injections of mikivan][0003]
    public CMoveSCU(String[] b,
                    String[] c,
                    String[] opts,
                    String   findLevel,
                    String[] m,
                    String[] args) throws IOException, ParseException {

        super("movescu");
        this.addConnection(conn);
        this.addApplicationEntity(ae);
        this.ae.addConnection(conn);

        //замена CLIUtils.configureConnect(this.remote, this.rq, cl);
        this.rq.setCalledAET(c[0]);
        this.remote.setHostname(c[1]);
        this.remote.setPort(Integer.parseInt(c[2]));
        //без настройки proxy, есть в CLIUtils.configureConnect

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
                false);

        //CommandLine cl = parseComandLine(args);

        //configureKeys(this, cl);
        this.addLevel(findLevel);
        CLIUtils.addAttributes(this.keys, m);

        //this.setPriority(CLIUtils.priorityOf(cl));
        //this.setDestination(destinationOf(cl));
        this.setPriority(0);

        this.isConstructorWithArgs = true;
    }


    public String doMove() throws  Exception {
        if(this.isConstructorWithArgs){

            this.setExecutor2( Executors.newSingleThreadExecutor() );
            this.setScheduledExecutor( Executors.newSingleThreadScheduledExecutor() );

            try {
                System.out.println("metod doMove() [1]");
                this.open();

                System.out.println("metod doMove() [2]");
                this.retrieve();

                System.out.println("state [1]------> " + this.as.getState().toString());

                this.close();
                this.getExecutor2().shutdown();
                this.getScheduledExecutor().shutdown();

                System.out.println("isClosed [1]= " + this.as.getSocket().isClosed());

                this.as.waitForSocketClose();

                System.out.println("isClosed [2]= " + this.as.getSocket().isClosed());

                System.out.println("state [3]------> " + this.as.getState().toString());

                this.isConstructorWithArgs = false;


                return this.as.getState().toString();//true;
            }
            catch (Exception e){

                return "false";

            }

        }
        else{

            return "false";
        }


    }
    //[end][0003]
    //------------------------------------------------------------------------------------------------------------------
    //------------------------------------------------------------------------------------------------------------------
    //------------------------------------------------------------------------------------------------------------------



    @SuppressWarnings("unchecked")
    public static void main(String[] args) {
// -b IVAN@192.168.121.101:49049 -c WATCHER@192.168.121.100:4006 -L STUDY -m StudyInstanceUID=1.2.840.113704.1.111.4156.1367430813.2 --explicit-vr --dest IVAN
// -b IVAN@192.168.121.101:49049 -c WATCHER@192.168.121.100:4006 -L STUDY -m StudyInstanceUID=1.3.46.670589.11.33435.5.0.28680.2017092509091596050 --explicit-vr --dest AE_TITLE
// -b IVAN@192.168.121.101:49049 -c WATCHER@192.168.121.100:4006 -L STUDY -m StudyInstanceUID=1.3.46.670589.11.33435.5.0.28680.2017092509091596050 --explicit-vr
//-b IVAN@192.168.121.101:49049 -c WATCHER@192.168.121.100:4006 -L STUDY -m StudyInstanceUID=1.3.46.670589.11.33435.5.0.28680.2017092509091596050 --explicit-vr --dest IVAN

        try{

            for (int i = 0; i < args.length ; i++ ) System.out.println( "args[" + i + "] = " + args[i] );

            String[] bind   = { "IVAN",    "192.168.121.101", "4006"};//строгий порядок
            String[] remote = { "WATCHER", "192.168.121.100", "4006"};//строгий порядок
//            String[] bind   = { "IVAN",   "192.168.0.74", "4006"};//строгий порядок
//            String[] remote = { "PACS01", "192.168.0.35", "4006"};//строгий порядок
            String[] opts   = {};
            String[] m      = { "StudyInstanceUID", "1.2.840.113704.1.111.4156.1367430813.2"};

            CMoveSCU main = new CMoveSCU(bind, remote, opts, "STUDY", m, args);

            System.out.println("main.doMove() = " + main.doMove());

            //System.in.read();

            System.out.println("===================");

//        } catch (ParseException e) {
//            System.err.println("movescu: " + e.getMessage());
//            System.err.println(rb.getString("try"));
//            System.exit(2);
        } catch (Exception e) {
            System.err.println("movescu: " + e.getMessage());
            e.printStackTrace();
            System.exit(2);
        };



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


//    private static void configureServiceClass(CMoveSCU main, CommandLine cl) throws ParseException {
//        main.setInformationModel(informationModelOf(cl),
//                CLIUtils.transferSyntaxesOf(cl), cl.hasOption("relational"));
//    }
//
//    private static String destinationOf(CommandLine cl) throws ParseException {
//        if (cl.hasOption("dest"))
//            return cl.getOptionValue("dest");
//        throw new ParseException(rb.getString("missing-dest"));
//    }
//
//    private static void configureKeys(CMoveSCU main, CommandLine cl) {
//        if (cl.hasOption("m")) {
//            String[] keys = cl.getOptionValues("m");
//            for (int i = 1; i < keys.length; i++, i++)
//                main.addKey(CLIUtils.toTag(keys[i - 1]), StringUtils.split(keys[i], '/'));
//        }
//        if (cl.hasOption("L"))
//            main.addLevel(cl.getOptionValue("L"));
//        if (cl.hasOption("i"))
//            main.setInputFilter(CLIUtils.toTags(cl.getOptionValues("i")));
//    }
//
//    private static InformationModel informationModelOf(CommandLine cl) throws ParseException {
//        try {
//            return cl.hasOption("M")
//                    ? InformationModel.valueOf(cl.getOptionValue("M"))
//                    : InformationModel.StudyRoot;
//        } catch(IllegalArgumentException e) {
//            throw new ParseException(MessageFormat.format(
//                    rb.getString("invalid-model-name"),
//                    cl.getOptionValue("M")));
//        }
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
    }

//    public void retrieve(File f) throws IOException, InterruptedException {
//        Attributes attrs = new Attributes();
//        DicomInputStream dis = null;
//        try {
//            attrs.addSelected(new DicomInputStream(f).readDataset(-1, -1), inFilter);
//        } finally {
//            SafeClose.close(dis);
//        }
//        attrs.addAll(keys);
//        retrieve(attrs);
//    }

    public void retrieve() throws IOException, InterruptedException {
        retrieve(keys);
    }

    private void retrieve(Attributes keys) throws IOException, InterruptedException {
        DimseRSPHandler rspHandler = new DimseRSPHandler(as.nextMessageID()) {

            @Override
            public void onDimseRSP(Association as, Attributes cmd,
                                   Attributes data) {
                super.onDimseRSP(as, cmd, data);
            }
        };

        as.cmove(model.cuid, priority, keys, null, destination, rspHandler);
    }

}

