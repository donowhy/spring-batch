package com.system.batch.origin.chapter3;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public interface ItemPreparedStatementSetter <T>{
    void setValues(T item, PreparedStatement ps) throws SQLException;
}
