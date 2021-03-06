//package lk.avix.ldap;
//
//import org.apache.directory.api.ldap.model.exception.LdapException;
//import org.apache.directory.api.ldap.model.exception.LdapInvalidDnException;
//import org.apache.directory.api.ldap.model.name.Dn;
//import org.apache.directory.api.ldap.model.schema.SchemaManager;
//import org.apache.directory.api.ldap.model.schema.registries.SchemaLoader;
//import org.apache.directory.api.ldap.schema.extractor.SchemaLdifExtractor;
//import org.apache.directory.api.ldap.schema.extractor.impl.DefaultSchemaLdifExtractor;
//import org.apache.directory.api.ldap.schema.loader.LdifSchemaLoader;
//import org.apache.directory.api.ldap.schema.manager.impl.DefaultSchemaManager;
//import org.apache.directory.api.util.exception.Exceptions;
//import org.apache.directory.server.constants.ServerDNConstants;
//import org.apache.directory.server.constants.SystemSchemaConstants;
//import org.apache.directory.server.core.DefaultDirectoryService;
//import org.apache.directory.server.core.api.DirectoryService;
//import org.apache.directory.server.core.api.DnFactory;
//import org.apache.directory.server.core.api.InstanceLayout;
//import org.apache.directory.server.core.api.partition.Partition;
//import org.apache.directory.server.core.api.schema.SchemaPartition;
//import org.apache.directory.server.core.partition.impl.btree.jdbm.JdbmPartition;
//import org.apache.directory.server.core.partition.ldif.LdifPartition;
//import org.apache.directory.server.ldap.LdapServer;
//import org.apache.directory.server.protocol.shared.store.LdifFileLoader;
//import org.apache.directory.server.protocol.shared.transport.TcpTransport;
//
//import java.io.File;
//import java.io.IOException;
//import java.util.List;
//
///**
// * Embedded Apache directory server implementation.
// */
//public class EmbeddedDirectoryServer {
//
//    private static final String SCHEMA_PARTITION_NAME = "schema";
//    private static final String LDAP_AUTH_TEST_PARTITION_NAME = "b7a";
//    private static final String LDAP_AUTH_TEST_PARTITION_DN = "dc=BALLERINA,dc=IO";
//    // The directory service
//    private DirectoryService service;
//    private LdapServer ldapServer;
//    private File workDir;
//
//    /**
//     * Add a new partition to the server.
//     *
//     * @param partitionId The partition Id
//     * @param partitionDn The partition DN
//     * @param dnFactory   The factory for DNs
//     * @return The newly added partition
//     * @throws Exception If the partition can't be added
//     */
//    private Partition addPartition(String partitionId, String partitionDn, DnFactory dnFactory) throws Exception {
//        JdbmPartition partition = new JdbmPartition(service.getSchemaManager(), dnFactory);
//        partition.setId(partitionId);
//        partition.setPartitionPath(new File(service.getInstanceLayout().getPartitionsDirectory(), partitionId).toURI());
//        partition.setSuffixDn(new Dn(service.getSchemaManager(), partitionDn));
//        return partition;
//    }
//
//    /**
//     * Starts directory apache directory service.
//     *
//     * @param port directory service port
//     * @throws Exception If server can't be started
//     */
//    public void startLdapServer(int port) throws Exception {
//
//        workDir = new File(System.getProperty("java.io.tmpdir"), "TEMP_APACHEDS-" + System.currentTimeMillis());
//        workDir.mkdirs();
//        // Initialize the LDAP service
//        ldapServer = new LdapServer();
//        ldapServer.setTransports(new TcpTransport(port));
//
//        service = new DefaultDirectoryService();
//        ldapServer.setDirectoryService(service);
//        service.setInstanceLayout(new InstanceLayout(workDir));
//
//        // Disable the ChangeLog system
//        service.getChangeLog().setEnabled(false);
//        service.setDenormalizeOpAttrsEnabled(true);
//
//        // Initialize a schema partition
//        initSchemaPartition();
//
//        // Initialize the system partition
//        initSystemPartition();
//
//        // Create a new partition named 'b7a'.
//        Partition b7aPartition = addPartition(LDAP_AUTH_TEST_PARTITION_NAME,
//                LDAP_AUTH_TEST_PARTITION_DN, service.getDnFactory());
//        service.addPartition(b7aPartition);
//
//        // Start the service
//        service.startup();
//        // Start the Ldap Server
//        ldapServer.start();
//
//        // Load the LDIF file
//        String ldif = new File(getClass().getClassLoader().getResource("users.ldif").getFile()).getAbsolutePath();
//        LdifFileLoader ldifLoader = new LdifFileLoader(service.getAdminSession(), ldif);
//        ldifLoader.execute();
//    }
//
//    private void initSchemaPartition() throws IOException, LdapException {
//        InstanceLayout instanceLayout = service.getInstanceLayout();
//        File schemaPartitionDirectory = new File(instanceLayout.getPartitionsDirectory(), SCHEMA_PARTITION_NAME);
//
//        if (schemaPartitionDirectory.exists()) {
//            System.out.println("Schema partition already exists, skipping schema extraction.");
//        } else {
//            SchemaLdifExtractor extractor = new DefaultSchemaLdifExtractor(instanceLayout.getPartitionsDirectory());
//            extractor.extractOrCopy();
//        }
//
//        SchemaLoader loader = new LdifSchemaLoader(schemaPartitionDirectory);
//        SchemaManager schemaManager = new DefaultSchemaManager(loader);
//        schemaManager.loadAllEnabled();
//        List<Throwable> errors = schemaManager.getErrors();
//
//        if (errors != null && !errors.isEmpty()) {
//            throw new LdapException(Exceptions.printErrors(errors));
//        }
//
//        LdifPartition schemaLdifPartition = new LdifPartition(schemaManager, service.getDnFactory());
//        schemaLdifPartition.setPartitionPath(schemaPartitionDirectory.toURI());
//        SchemaPartition schemaPartition = new SchemaPartition(schemaManager);
//        schemaPartition.setWrappedPartition(schemaLdifPartition);
//        service.setSchemaManager(schemaManager);
//        service.setSchemaPartition(schemaPartition);
//    }
//
//    private void initSystemPartition() throws LdapInvalidDnException {
//        JdbmPartition systemPartition = new JdbmPartition(service.getSchemaManager(), service.getDnFactory());
//        systemPartition.setId(SystemSchemaConstants.SCHEMA_NAME);
//        systemPartition.setPartitionPath(new File(service.getInstanceLayout().getPartitionsDirectory(),
//                systemPartition.getId()).toURI());
//        systemPartition.setSuffixDn(new Dn(ServerDNConstants.SYSTEM_DN));
//        systemPartition.setSchemaManager(service.getSchemaManager());
//        service.setSystemPartition(systemPartition);
//    }
//
//    /**
//     * Stops directory apache directory service.
//     */
//    void stopLdapService() {
//        ldapServer.stop();
//        workDir.delete();
//    }
//
//    public static void main(String[] args) throws Exception {
//        EmbeddedDirectoryServer e = new EmbeddedDirectoryServer();
//        e.startLdapServer(20000);
//    }
//}
