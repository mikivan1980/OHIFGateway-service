package com.mikivan.service;

        import java.io.*;
        import java.util.concurrent.Executors;
        import org.dcm4che3.data.Tag;
        import org.dcm4che3.data.Attributes;
        //import org.dcm4che3.data.UID;
        import org.dcm4che3.data.VR;
        //import org.dcm4che3.io.DicomInputStream;
        import org.dcm4che3.io.DicomOutputStream;
        //import org.dcm4che3.io.DicomInputStream.IncludeBulkData;
        import org.dcm4che3.net.*;
        import org.dcm4che3.net.pdu.PresentationContext;
        import org.dcm4che3.net.service.BasicCEchoSCP;
        import org.dcm4che3.net.service.BasicCStoreSCP;
        import org.dcm4che3.net.service.DicomServiceException;
        import org.dcm4che3.net.service.DicomServiceRegistry;
        import org.dcm4che3.util.SafeClose;
        //import org.slf4j.Logger;
        //import org.slf4j.LoggerFactory;
        import java.util.Map;
        import java.util.HashMap;


public class CStoreSCP {

    private boolean isConstructorWithArgs = false;

    public static long count = 0;
    public static Map<String, byte[]> dataList = new HashMap<String,  byte[]>();

//    private static final Logger LOG = LoggerFactory.getLogger(CStoreSCP.class);

    private final Device device = new Device("storescp");
    private final ApplicationEntity ae = new ApplicationEntity("*");
    private final Connection conn = new Connection();
    private int status;


    private final BasicCStoreSCP cstoreSCP = new BasicCStoreSCP("*") {

        @Override
        protected void store(Association as, PresentationContext pc,
                             Attributes rq, PDVInputStream data, Attributes rsp)
                throws IOException {

            rsp.setInt(Tag.Status, VR.US, status);


/* диком объекты (файлы) находящиеся в хранилище диком сервера имеют
* ts = UID.ImplicitVRLittleEndian = "1.2.840.10008.1.2";
* так же поступает и CStoreSCP, когда файл записыватся на диск или wado, то
* диком объект имеет ts = UID.ExplicitVRLittleEndian = = "1.2.840.10008.1.2.1";
* OHIF ест все форматы, так что можно не преобразовывать
* */

                // свой код обработки принятых данных
                // помещаем наши ланные в статические коллекции т.е. одну на все вызовы.
                String cuid = rq.getString(Tag.AffectedSOPClassUID);
                String iuid = rq.getString(Tag.AffectedSOPInstanceUID);
                String tsuid = pc.getTransferSyntax();

                System.out.println("tsuid ===> " + tsuid);

                ByteArrayOutputStream out = new ByteArrayOutputStream();

                ////BufferedOutputStream outBuffer = new BufferedOutputStream(out);
                ////DicomOutputStream dicomVirtualFile = new DicomOutputStream(outBuffer , UID.ExplicitVRLittleEndian);

                //DicomOutputStream dicomVirtualFile = new DicomOutputStream(out , UID.ExplicitVRLittleEndian);
                DicomOutputStream dicomVirtualFile = new DicomOutputStream(new File("d:\\workshop\\INTELLOGIC\\development\\cstore-output\\out" + count + ".dcm"));

                try {

                    try{

                        dicomVirtualFile.writeFileMetaInformation( as.createFileMetaInformation(iuid, cuid, tsuid) );
                        data.copyTo(dicomVirtualFile);

                    } finally {

                        dataList.put(iuid, out.toByteArray());

                        System.out.println("out.toByteArray().length ===> " + out.toByteArray().length);

                        SafeClose.close(dicomVirtualFile);
                        out.close();

                        System.out.println("count ==>>> " + count + ";    dataList.size() = " + dataList.size());

                        count++;
                        return;

                    } } catch (Exception e) {
                    throw new DicomServiceException(Status.ProcessingFailure, e);
                }

        }

    };

    private DicomServiceRegistry createServiceRegistry() {
        DicomServiceRegistry serviceRegistry = new DicomServiceRegistry();
        serviceRegistry.addDicomService(new BasicCEchoSCP());
        serviceRegistry.addDicomService(cstoreSCP);
        return serviceRegistry;
    }

    public void setStatus(int status) {
        this.status = status;
    }


    //------------------------------------------------------------------------------------------------------------------
    //------------------------------------------------------------------------------------------------------------------
    //------------------------------------------------------------------------------------------------------------------
    public CStoreSCP(String[] b, String[] opts, String [] args) throws Exception {

        this.device.setDimseRQHandler(createServiceRegistry());
        this.device.addConnection(conn);
        this.device.addApplicationEntity(ae);
        this.ae.setAssociationAcceptor(true);
        this.ae.addConnection(conn);

        //CLIUtils.configureBindServer(this.conn, this.ae, cl);
        this.ae.setAETitle(b[0]);
        this.conn.setHostname(b[1]);
        this.conn.setPort(Integer.parseInt(b[2]));

        //CLIUtils.configure(this.conn, cl);
        configure(this.conn, opts);

        //this.setStatus(CLIUtils.getIntOption(cl, "status", 0));
        // статус по умолчанию 0 т.к. не используем опцию "status"
        this.setStatus(0);

        //configureTransferCapability(this.ae, cl);
        ae.addTransferCapability(
            new TransferCapability(null, "*", TransferCapability.Role.SCP,"*"));


        this.device.setExecutor2( Executors.newCachedThreadPool() );
        this.device.setScheduledExecutor( Executors.newSingleThreadScheduledExecutor() );

        this.device.bindConnections();

        this.isConstructorWithArgs = true;
    }

    public void stop(){
        if(isConstructorWithArgs){

            this.device.getExecutor2().shutdown();
            this.device.getScheduledExecutor().shutdown();

            this.isConstructorWithArgs = false;
        }

    }
    //------------------------------------------------------------------------------------------------------------------
    //------------------------------------------------------------------------------------------------------------------
    //------------------------------------------------------------------------------------------------------------------


    public static void main(String[] args) {
        try {

//-b IVAN@192.168.0.74:4006 --not-async  --sop-classes "D:\dop\java\master-mikivan-dcm4che\dcm4che-assembly\src\etc\storescp\sop-classes.properties" --directory "d:\workshop\INTELLOGIC\development\cstore-output\"
//-b IVAN@192.168.0.74:4006 --not-async  --sop-classes "D:\dop\java\master-mikivan-dcm4che\dcm4che-assembly\src\etc\storescp\sop-classes.properties" --directory ""
//-b IVAN@192.168.121.101:4006 --not-async --sop-classes "D:\dop\java\master-mikivan-dcm4che\dcm4che-assembly\src\etc\storescp\sop-classes.properties"
//-b IVAN@192.168.121.101:4006 --not-async --accept-unknown

            for (int i = 0; i < args.length ; i++ ) System.out.println( "args[" + i + "] = " + args[i] );

            String[] bind   = { "IVAN", "192.168.0.74", "4006"};//строгий порядок
            //String[] remote = { "WATCHER", "192.168.121.100", "4006"};//строгий порядок
            String[] opts   = {};

            CStoreSCP main = new CStoreSCP(bind, opts, args);

            System.out.println("store scp running ... ");


        } catch (Exception e) {
            System.err.println("storescp: " + e.getMessage());
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


//    private static void configureTransferCapability(ApplicationEntity ae,
//                                                    CommandLine cl) throws IOException {
//        if (cl.hasOption("accept-unknown")) {
//            ae.addTransferCapability(
//                    new TransferCapability(null,
//                            "*",
//                            TransferCapability.Role.SCP,
//                            //UID.ImplicitVRLittleEndian));
//                            //UID.ExplicitVRLittleEndian));// mikivan for wado
//                            "*"));//mikivan
//        } else {
//            Properties p = CLIUtils.loadProperties(
//                    cl.getOptionValue("sop-classes",
//                            "resource:sop-classes.properties"),
//                    null);
//            for (String cuid : p.stringPropertyNames()) {
//                String ts = p.getProperty(cuid);
//                TransferCapability tc = new TransferCapability(null,
//                        CLIUtils.toUID(cuid),
//                        TransferCapability.Role.SCP,
//                        CLIUtils.toUIDs(ts));
//                ae.addTransferCapability(tc);
//            }
//        }
//    }

}

