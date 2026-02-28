package com.system.batch.origin.chapter3;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;

public class VictimPreparedStatementSetter implements ItemPreparedStatementSetter<Victim>{
    @Override
    public void setValues(Victim victim, PreparedStatement ps) throws SQLException {
        ps.setLong(1, victim.getId());
        ps.setString(2, victim.getName());
        ps.setString(3, victim.getProcessId());
        ps.setTimestamp(3,  Timestamp.valueOf(victim.getTerminatedAt()));
        ps.setString(5, victim.getStatus());
    }
}
