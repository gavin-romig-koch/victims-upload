<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"
	import="java.util.*,javax.naming.*,javax.sql.DataSource,java.sql.*"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>DS Test</title>
</head>
<body>
	<%
		DataSource ds = null;
		Connection con = null;
		InitialContext ic;
		try {
			ic = new InitialContext();
			ds = (DataSource) ic.lookup("java:jboss/datasources/CloudConsolePSQLDS");

			con = ds.getConnection();
			DatabaseMetaData dbmd = con.getMetaData();  
			
			out.println("<pre>");
			out.println("=====  Database info =====");  
			out.println("DatabaseProductName: " + dbmd.getDatabaseProductName());  
			out.println("DatabaseProductVersion: " + dbmd.getDatabaseProductVersion() );  
			out.println("DatabaseMajorVersion: " + dbmd.getDatabaseMajorVersion() );  
			out.println("DatabaseMinorVersion: " + dbmd.getDatabaseMinorVersion() );  
			out.println("=====  Driver info =====");  
			out.println("DriverName: " + dbmd.getDriverName() );  
			out.println("DriverVersion: " + dbmd.getDriverVersion() );  
			out.println("DriverMajorVersion: " + dbmd.getDriverMajorVersion() );  
			out.println("DriverMinorVersion: " + dbmd.getDriverMinorVersion() );  
			out.println("=====  JDBC/DB attributes =====");
			out.println("</pre>");
		} catch (Exception e) {
			out.println("Exception thrown ");
			e.printStackTrace();
		} finally {
			if (con != null) {
				con.close();
			}
		}
	%>
</body>
</html>