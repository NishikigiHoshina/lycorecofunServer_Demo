package com.lycorisfun.fun.util;


import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class JdbcUtils {

        private static String url;
        private static String user;
        private static String password;

        // 初始化数据库连接参数
        static {
            url = "jdbc:mysql://localhost:3306/Demo?allowPublicKeyRetrieval=true&useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai";

            user = "root";
            password = "dxx5201314";

            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }

        // 获取数据库连接
        public static Connection getConnection() throws SQLException {
            return DriverManager.getConnection(url, user, password);
        }

        // 关闭资源
        public static void close(ResultSet rs, Statement stmt, Connection conn) {
            if (rs!= null) {
                try {
                    rs.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            if (stmt!= null) {
                try {
                    stmt.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            if (conn!= null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }

        // 执行增删改操作
        public static int executeUpdate(String sql, Object... params) throws SQLException {
            Connection conn = getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql);
            for (int i = 0; i < params.length; i++) {
                pstmt.setObject(i + 1, params[i]);
            }
            int rows = pstmt.executeUpdate();
            close(null, pstmt, conn);
            return rows;
        }

        // 执行查询操作
        public static ResultSet executeQuery(String sql, Object... params) throws SQLException {
            Connection conn = getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql);
            for (int i = 0; i < params.length; i++) {
                pstmt.setObject(i + 1, params[i]);
            }
            ResultSet rs = pstmt.executeQuery();
            return rs;
        }
    }

