package Utils;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.parquet.avro.AvroParquetWriter;
import org.apache.parquet.hadoop.ParquetWriter;
import org.apache.parquet.hadoop.metadata.CompressionCodecName;

import java.io.File;
import java.sql.*;
import java.time.Instant;
import java.util.*;


public class SimpleDataExtract implements AutoCloseable {
    private final HikariDataSource ds;
    private final int fetchSize;

    public SimpleDataExtract(Properties dbProps, int fetchSize) {
        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl(dbProps.getProperty("jdbc.url"));
        cfg.setUsername(dbProps.getProperty("jdbc.user"));
        cfg.setPassword(dbProps.getProperty("jdbc.password"));
        cfg.addDataSourceProperty("cachePrepStmts", "true");
        cfg.addDataSourceProperty("prepStmtCacheSize", "250");
        cfg.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        this.ds = new HikariDataSource(cfg);
        this.fetchSize = fetchSize;
    }

    public interface RowConsumer {
        void accept(ResultSet rs) throws Exception;
    }

    public void streamQuery(String sql, List<Object> params, RowConsumer consumer) throws Exception {
        try (Connection conn = ds.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)) {
                ps.setFetchSize(fetchSize);
                setParams(ps, params);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) consumer.accept(rs);
                }
            }
        }
    }

    public void exportQueryToParquet(
            String sql,
            List<Object> params,
            File outFile,
            Set<String> fieldsToAnonymize) throws Exception {

        try (Connection conn = ds.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)) {
                ps.setFetchSize(fetchSize);
                setParams(ps, params);
                try (ResultSet rs = ps.executeQuery()) {
                    ResultSetMetaData md = rs.getMetaData();
                    Schema schema = buildSchemaFromMeta(md);

                    Configuration conf = new Configuration();
                    Path path = new Path(outFile.getAbsolutePath());
                    try (ParquetWriter<GenericRecord> writer = AvroParquetWriter.<GenericRecord>builder(
                                    org.apache.parquet.hadoop.util.HadoopOutputFile.fromPath(path, conf))
                            .withSchema(schema)
                            .withCompressionCodec(CompressionCodecName.SNAPPY)
                            .build()) {

                        final int cols = md.getColumnCount();
                        while (rs.next()) {
                            GenericRecord rec = new GenericData.Record(schema);
                            for (int c = 1; c <= cols; c++) {
                                String name = md.getColumnLabel(c);
                                int sqlType = md.getColumnType(c);
                                Object val = readTypedValue(rs, c, sqlType);

                                if (fieldsToAnonymize != null && fieldsToAnonymize.contains(name)) {
                                    if (val instanceof Integer || val instanceof Long) {
                                        long id = ((Number) val).longValue();
                                        val = IdAnonymizer.anonymizeId(id);
                                    }
                                }

                                rec.put(name, val);
                            }
                            writer.write(rec);
                        }
                    }
                }
            }
        }
    }


    private static Schema buildSchemaFromMeta(ResultSetMetaData md) throws SQLException {
        String recordName = "Row";
        Schema record = Schema.createRecord(recordName, null, null, false);
        List<Schema.Field> fields = new ArrayList<>();
        int cols = md.getColumnCount();
        for (int i = 1; i <= cols; i++) {
            String name = md.getColumnLabel(i);
            int sqlType = md.getColumnType(i);
            Schema base = sqlTypeToAvroSchema(sqlType);
            List<Schema> union = Arrays.asList(Schema.create(Schema.Type.NULL), base);
            Schema unionSchema = Schema.createUnion(union);
            Schema.Field f = new Schema.Field(name, unionSchema, null, Schema.Field.NULL_DEFAULT_VALUE);
            fields.add(f);
        }
        record.setFields(fields);
        return record;
    }

    private static Schema sqlTypeToAvroSchema(int sqlType) {
        switch (sqlType) {
            case Types.INTEGER:
            case Types.SMALLINT:
            case Types.TINYINT:
                return Schema.create(Schema.Type.INT);
            case Types.BIGINT:
                return Schema.create(Schema.Type.LONG);
            case Types.FLOAT:
            case Types.REAL:
            case Types.DOUBLE:
            case Types.NUMERIC:
            case Types.DECIMAL:
                return Schema.create(Schema.Type.DOUBLE);
            case Types.BOOLEAN:
            case Types.BIT:
                return Schema.create(Schema.Type.BOOLEAN);
            case Types.DATE:
            case Types.TIME:
            case Types.TIMESTAMP:
            default:
                return Schema.create(Schema.Type.STRING);
        }
    }

    private static Object readTypedValue(ResultSet rs, int colIndex, int sqlType) throws SQLException {
        switch (sqlType) {
            case Types.INTEGER:
            case Types.SMALLINT:
            case Types.TINYINT:
                int i = rs.getInt(colIndex);
                return rs.wasNull() ? null : i;
            case Types.BIGINT:
                long l = rs.getLong(colIndex);
                return rs.wasNull() ? null : l;
            case Types.FLOAT:
            case Types.REAL:
            case Types.DOUBLE:
            case Types.NUMERIC:
            case Types.DECIMAL:
                double d = rs.getDouble(colIndex);
                return rs.wasNull() ? null : d;
            case Types.BOOLEAN:
            case Types.BIT:
                boolean b = rs.getBoolean(colIndex);
                return rs.wasNull() ? null : b;
            case Types.TIMESTAMP:
            case Types.DATE:
            case Types.TIME:
                Timestamp ts = rs.getTimestamp(colIndex);
                return ts == null ? null : Instant.ofEpochMilli(ts.getTime()).toString(); // ISO
            default:
                String s = rs.getString(colIndex);
                return s;
        }
    }

    private static void setParams(PreparedStatement ps, List<Object> params) throws SQLException {
        if (params == null) return;
        for (int i = 0; i < params.size(); i++) ps.setObject(i + 1, params.get(i));
    }

    public HikariDataSource getDataSource() {
        return ds;
    }

    @Override
    public void close() {
        if (ds != null) ds.close();
    }
}
