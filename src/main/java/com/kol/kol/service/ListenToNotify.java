// package com.kol.kol.service;

// import java.sql.Statement;

// import com.impossibl.postgres.api.jdbc.PGConnection;
// import com.impossibl.postgres.api.jdbc.PGNotificationListener;
// import com.impossibl.postgres.jdbc.PGDataSource;

// public class ListenToNotify {
    
    
//     public static void listenToNotifyMessage(){
//         PGDataSource dataSource = new PGDataSource();
//         dataSource.setHost("localhost");
//         dataSource.setPort(5432);
//         dataSource.setDatabaseName("kol");
        
//         PGNotificationListener listener = new PGNotificationListener() {

//             @Override
//             public void notification(int processId, String channelName, String payload) {
//                 System.out.println("notifications: " + payload);
//             }
//         };
        
//         try (PGConnection connection = (PGConnection) dataSource.getConnection()){
//             Statement statement = connection.createStatement();
//             statement.execute("LISTEN update_notification");
//             statement.close();
//             connection.addNotificationListener(listener);

//             while (true){ }
//         } catch (Exception e) {
//             System.err.println(e);
//         }
//     }
// }
