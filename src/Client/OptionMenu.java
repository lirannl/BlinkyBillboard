package Client;

import SocketCommunication.Request;
import SocketCommunication.RequestType;
import SocketCommunication.Session;

import javax.swing.*;

public class OptionMenu extends JFrame {

    JPanel panel = new JPanel();
    JButton listAllBillboardsButton = new JButton("List All Billboards");
    JButton createBillboardsButton = new JButton("Create Billboards");
    JButton editAllBillboardsButton = new JButton("Edit All Billboards");
    JButton scheduleBillboardsButton = new JButton("Schedule Billboards");
    JButton editUsersButton = new JButton("Edit Users");

    public OptionMenu(Session session) {
        super("Option Menu");
        setSize(300, 200);
        setLocation(500, 280);
        panel.setLayout(null);


        listAllBillboardsButton.setBounds(70, 25, 150, 30);
        panel.add(listAllBillboardsButton);
        actionListBillboards(session);

            if (session.canCreateBillboards) {
                createBillboardsButton.setBounds(70, 25, 150, 30);
                panel.add(createBillboardsButton);
                actionCreateBillboards(session);
            }
            else if (session.editAllBillboards) {
                editAllBillboardsButton.setBounds(70, 50, 150, 30);
                panel.add(editAllBillboardsButton);
                actionEditAllBillboards();
            }
            else if (session.scheduleBillboards) {
                scheduleBillboardsButton.setBounds(70, 75, 150, 30);
                panel.add(scheduleBillboardsButton);
                actionScheduleBillboards();
            }
            else if (session.editUsers) {
                editUsersButton.setBounds(70, 100, 150, 30);
                panel.add(editUsersButton);
                actionEditUsers();

        }

        getContentPane().add(panel);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);
    }

    public void actionListBillboards(Session session) {
        listAllBillboardsButton.addActionListener(ae -> {
            System.out.println("Testing list billboards button");
            Request.serverRequest(RequestType.LIST_BILL_REQ,session);
        });
    }

    public void actionCreateBillboards(Session session) {
        createBillboardsButton.addActionListener(ae -> {
            System.out.println("Testing create billboards button");
            //This will take you to the Create billboard GUI
            //Path to create billboards GUI here, GUI once submit button clicked will send createBillBoard request
        });
    }
    public void actionScheduleBillboards() {
        scheduleBillboardsButton.addActionListener(ae -> {
            System.out.println("Testing Schedule Billboards button");
        });
    }

    public void actionEditAllBillboards() {
        editAllBillboardsButton.addActionListener(ae -> {
            System.out.println("Testing edit all billboards button");
        });
    }

    public void actionEditUsers() {
        editUsersButton.addActionListener(ae -> {
            System.out.println("Testing edit users button");
            //Call the list, create, and edit users GUI
        });
    }
}