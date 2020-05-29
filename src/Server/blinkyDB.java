package Server;

import BillboardSupport.Billboard;
import BillboardSupport.DummyBillboards;
import SocketCommunication.Credentials;
import SocketCommunication.Response;

import javax.xml.transform.Result;
import java.awt.*;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.List;
import java.util.function.Function;

public class blinkyDB {
    private DBProps props;
    private Connection dbconn;

    /**
     * Database object constructor
     * @throws IOException If db.props isn't found
     * @throws SQLException If there's a problem connecting to the database
     */
    public blinkyDB(Boolean dropSchema) throws IOException, SQLException { // Create a new database object - attempting to populate an actual database if one isn't already initialised. Then, start a connection to the database.
        props = new DBProps(); // Read db.props
        // Ensure the schema exists
        Connection init_schema = DriverManager.getConnection("jdbc:mariadb://"+props.url+":3306/", props.username, props.password);
        if (dropSchema) init_schema.createStatement().executeQuery("DROP DATABASE IF EXISTS " + props.schema);
        init_schema.createStatement().executeQuery("CREATE DATABASE IF NOT EXISTS " + props.schema);
        init_schema.close();
        // Start a database connection
        dbconn = DriverManager.getConnection("jdbc:mariadb://"+props.url+":3306/"+props.schema, props.username, props.password);
        // Try to initialise based on sql file
        Path sqlInitFile = Paths.get(new File("blinkybillboard.sql").getPath());
        String[] batch = new String(Files.readAllBytes(sqlInitFile)).split("(?<=;)");
            // Execute all statements in the array
        for (String toExec:batch) {
            if (!toExec.trim().isEmpty()) // (don't execute empty statements)
            dbconn.createStatement().executeQuery(toExec);
        }
    }

    public blinkyDB() throws IOException, SQLException {
        blinkyDB newDB = new blinkyDB(false);
        this.dbconn = newDB.dbconn;
        this.props = newDB.props;
    }

    public List<Billboard> getBillboards(String searchQuery, String searchType) throws SQLException {
        PreparedStatement getBillboards;
        final String billboardLookUpString = (searchQuery != null && searchType != null) ?
                "select * from Billboards where ? like \"%?%\"" : "select * from Billboards";
        dbconn.setAutoCommit(false);
        getBillboards = dbconn.prepareStatement(billboardLookUpString);
        if (billboardLookUpString.contains("?"))
        {
            getBillboards.setString(1, searchType);
            getBillboards.setString(2, searchQuery);
        }
        dbconn.setAutoCommit(true);
        ResultSet rs = getBillboards.executeQuery();
        List<Billboard> BillboardList = new ArrayList<>();

        while (rs.next()){
            // For each returned billboard from the database
            Object image;
            try{
                ByteArrayInputStream bis = new ByteArrayInputStream(rs.getBytes("billboardImage"));
                ObjectInput in = new ObjectInputStream(bis);
                image = in.readObject();
            }
            catch (Exception e){
                image = null;
            }
            Billboard current = new Billboard();
            current.setBillboardDatabaseKey(rs.getInt("billboard_id"));
            current.setCreator(rs.getString("creator"));
            current.setBackgroundColour(new Color(rs.getInt("backgroundColour")));
            current.setMessageColour(new Color(rs.getInt("messageColour")));
            current.setInformationColour(new Color(rs.getInt("informationColour")));
            current.setMessage(rs.getString("message"));
            current.setInformation(rs.getString("information"));
            current.setImageData((String) image);
            BillboardList.add(current);
        }
        return BillboardList;
    }

    public List<Billboard> getBillboards() throws SQLException {
        return this.getBillboards(null, null);
    }

    public void CreateViewer(String socket) throws SQLException {
        String ViewerCreationString = "INSERT INTO blinkyBillboard.Viewers\n" +
                "(socket)\n" +
                "VALUES(?);\n";
        PreparedStatement ViewerInserter = dbconn.prepareStatement(ViewerCreationString);
        dbconn.setAutoCommit(false);
        ViewerInserter.setString(1, socket);
        ViewerInserter.executeUpdate();
        try{dbconn.commit();}
        catch (SQLException e){
            dbconn.rollback();
        }
        dbconn.setAutoCommit(true);
    }

    /**
     * Allows writing a billboard into the database
     * @param billboard_in The billboard to write to the database
     * @param creator The username of the billboard's creator
     * @throws SQLException If the creation fails
     */
    public void createBillboard(Billboard billboard_in, String creator) throws SQLException {
        assert creator != null;
        // Takes a property retriever for Billboards, and applies it to either the given billboard, or a default billboard object
        Function<Function<Billboard, Object>, Object> getPropertySafely = (Function<Billboard, Object> m) ->
                Objects.requireNonNullElse(m.apply(billboard_in), m.apply(DummyBillboards.defaultBillboard()));
        String BillboardInsertQuery = "INSERT INTO Billboards\n" +
                "(creator, backgroundColour, messageColour, informationColour, message, information, billboardImage)\n" +
                "VALUES(?, ?, ?, ?, ?, ?, ?);\n";
        dbconn.setAutoCommit(false);
        byte[] SerialisedImage;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            new ObjectOutputStream(bos).writeObject(getPropertySafely.apply(Billboard::getBillboardImage));
            SerialisedImage = bos.toByteArray();
        } catch (IOException e) { SerialisedImage = new byte[0]; }
        PreparedStatement insertBillboard = dbconn.prepareStatement(BillboardInsertQuery);
        try {
            insertBillboard.setString(1, creator);
            insertBillboard.setInt(2, ((Color)getPropertySafely.apply(Billboard::getBackgroundColour)).getRGB());
            insertBillboard.setInt(3, ((Color)getPropertySafely.apply(Billboard::getMessageColour)).getRGB());
            insertBillboard.setInt(4, ((Color)getPropertySafely.apply(Billboard::getInformationColour)).getRGB());
            insertBillboard.setString(5, ((String)getPropertySafely.apply(Billboard::getMessage)));
            insertBillboard.setString(6, ((String)getPropertySafely.apply(Billboard::getInformation)));
            insertBillboard.setBytes(7, SerialisedImage);
            insertBillboard.executeUpdate();
            dbconn.commit();
        } catch (SQLException e) {
            dbconn.rollback();
        }
        dbconn.setAutoCommit(true);
    }

    /**
     * A method for the User constructor to read user data from the database
     * @param username Username
     * @return The details of said user
     * @throws SQLException If the user lookup fails
     */
    protected ResultSet LookUpUserDetails(String username) throws SQLException {
        PreparedStatement UserLookUp; // Create the prepared statement object
        String userLookUpString = "select * from Users where user_name = ?"; // Define the query to run
        dbconn.setAutoCommit(false);

        UserLookUp = dbconn.prepareStatement(userLookUpString); // Compile the statement

        UserLookUp.setString(1, username); // Insert the provided username into the query

        dbconn.setAutoCommit(true);
        return UserLookUp.executeQuery(); // Run the query
    }

    protected ResultSet LookUpAllUserDetails() throws SQLException {
        PreparedStatement UserLookUp; // Create the prepared statement object
        String userLookUpString = "select * from Users"; // Define the query to run
        dbconn.setAutoCommit(false);

        UserLookUp = dbconn.prepareStatement(userLookUpString); // Compile the statement

        dbconn.setAutoCommit(true);
        return UserLookUp.executeQuery(); // Run the query
    }

    /**
     * Get all schedules from the specified start time onwards
     * @param time The earliest start time to get schedules of
     * @param id The billboard ID to get schedules for
     * @return The schedules
     * @throws SQLException If the lookup fails
     */
    public ResultSet getSchedules(LocalDateTime time, int id) throws SQLException {
        String filterString = id == -1 ? "" : "AND billboard_id = ?";
        String scheduleLookup = "SELECT * FROM Scheduling WHERE start_time < ?" + filterString;
        PreparedStatement ScheduleLookUp;
        dbconn.setAutoCommit(false);
        ScheduleLookUp = dbconn.prepareStatement(scheduleLookup);
        ScheduleLookUp.setTimestamp(1, Timestamp.valueOf(time));
        if (filterString.contains("?")) ScheduleLookUp.setInt(2, id);
        dbconn.setAutoCommit(true);
        return ScheduleLookUp.executeQuery();
    }

    /**
     * Get all schedules from the specified start time onwards
     * @param time The earliest start time to get schedules of
     * @return The schedules
     * @throws SQLException If the lookup fails
     */
    public ResultSet getSchedules(LocalDateTime time) throws SQLException {
        return getSchedules(time, -1);
    }

    /**
     * This method takes new user details and adds them to the database.
     * @param credentials The new user's credentials
     * @param CanCreateBillboards permission
     * @param EditAllBillBoards permission
     * @param ScheduleBillboards permission
     * @param EditUsers permission
     * @throws SQLException // If the insertion fails, such as in the case when such a user already exists in the database.
     */
    protected void RegisterUserInDatabase(Credentials credentials, boolean CanCreateBillboards, boolean EditAllBillBoards, boolean ScheduleBillboards, boolean EditUsers) throws SQLException {
        char[] permissions = new char[4];
        if (CanCreateBillboards) permissions[0] = 'B';
        if (EditAllBillBoards) permissions[1] = 'E';
        if (ScheduleBillboards) permissions[2] = 'S';
        if (EditUsers) permissions[3] = 'U';

        // Generate a random salt
        byte[] salt = new byte[100];
        new Random().nextBytes(salt);

        String userInsertionString = "INSERT INTO Users\n(user_name, user_permissions, password_hash, salt)\nVALUES(?, ?, ?, ?);";
        byte[] Salted = AuthenticationHandler.HashPasswordHashSalt(credentials.getPasswordHash(), salt);

        dbconn.setAutoCommit(false);

        PreparedStatement UserInserter = dbconn.prepareStatement(userInsertionString); // Prepare the insertion statement
        try {
            UserInserter.setString(1, credentials.getUsername());
            UserInserter.setString(2, new String(permissions));
            UserInserter.setBytes(3, Salted);
            UserInserter.setBytes(4, salt);

            UserInserter.executeUpdate();
            dbconn.commit();
        }
        catch (SQLException e) {
            dbconn.rollback(); // Try to rollback
            throw e;
        }
        dbconn.setAutoCommit(true);
    }

    /**
     * Updates user details
     * @param user User object to update the details of
     * @throws SQLException when the update fails
     */
    protected void UpdateUserDetails(User user) throws SQLException {
        char[] permissions = new char[4];
        if (user.CanCreateBillboards()) permissions[0] = 'B';
        if (user.CanEditAllBillboards()) permissions[1] = 'E';
        if (user.CanScheduleBillboards()) permissions[2] = 'S';
        if (user.CanEditUsers()) permissions[3] = 'U';

        String userUpdateString = "UPDATE Users\n" +
                "SET user_permissions=?, password_hash=?, salt=?\n" +
                "WHERE user_name=?;";

        dbconn.setAutoCommit(false);
        PreparedStatement updateUser = dbconn.prepareStatement(userUpdateString);
        try{
            updateUser.setString(1, new String(permissions));
            updateUser.setBytes(2, user.getSaltedCredentials().getPasswordHash());
            updateUser.setBytes(3, user.salt);
            updateUser.setString(4, user.getSaltedCredentials().getUsername());

            updateUser.executeUpdate();
            dbconn.commit();
        }
        catch (SQLException e){
            dbconn.rollback();
        }
        dbconn.setAutoCommit(true);
    }

    /**
     * Register a viewer in the database
     * @param Socket The viewer's socket (IP + port)
     * @throws SQLException If the registration fails
     */
    protected void RegisterViewer(String Socket) throws SQLException {
        String ViewerInserterString = "INSERT INTO Viewers (socket) VALUES(?);";
        PreparedStatement ViewerInserter = dbconn.prepareStatement(ViewerInserterString);

        dbconn.setAutoCommit(false);
        try {
            ViewerInserter.setString(1, Socket);

            ViewerInserter.executeUpdate();
            dbconn.commit();
        }
        catch (SQLException e){
            dbconn.rollback();
        }
        dbconn.setAutoCommit(true);
    }

    /**
     * Remove a viewer from the database
     * @param id The id of the viewer to delete
     * @throws SQLException If the deletion fails
     */
    protected void UnRegisterViewer(int id) throws SQLException {
        String ViewerDeleterString = "DELETE FROM Viewers\n" +
                "WHERE viewer_id=?;\n";
        PreparedStatement ViewerDeleter = dbconn.prepareStatement(ViewerDeleterString);

        dbconn.setAutoCommit(false);
        try{
            ViewerDeleter.setInt(1, id);
            ViewerDeleter.executeUpdate();
            dbconn.commit();
        }
        catch (SQLException e){
            dbconn.rollback();
        }
        dbconn.setAutoCommit(true);
    }

    /**
     * Get all viewers with their sockets
     * @return The viewers
     * @throws SQLException If the lookup fails
     */
    protected ResultSet GetViewers() throws SQLException {
        String ViewersSelectorString = "SELECT viewer_id, socket\n" +
                "FROM Viewers;\n";
        PreparedStatement ViewersSelector = dbconn.prepareStatement(ViewersSelectorString);
        return ViewersSelector.executeQuery();
    }
}
