package org.opencb.commons.bioformats.commons.core.connectors.variant.writers;

import org.bioinfo.commons.utils.StringUtils;
import org.opencb.commons.bioformats.commons.core.variant.vcf4.Genotype;
import org.opencb.commons.bioformats.commons.core.variant.vcf4.VcfRecord;
import org.opencb.commons.bioformats.commons.core.vcfstats.*;

import java.sql.*;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: aaleman
 * Date: 8/30/13
 * Time: 1:51 PM
 * To change this template use File | Settings | File Templates.
 */
public class VcfSqliteDataWriter implements VcfDataWriter {

    private String dbName;
    private Connection con;
    private Statement stmt;
    private PreparedStatement pstmt;
    private boolean createdSampleTable;


    public VcfSqliteDataWriter(String dbName) {
        this.dbName = dbName;
        stmt = null;
        pstmt = null;
        createdSampleTable = false;
    }

    @Override
    public boolean open() {

        try {
            Class.forName("org.sqlite.JDBC");
            con = DriverManager.getConnection("jdbc:sqlite:" + dbName);
            con.setAutoCommit(false);

        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
            return false;
        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
            return false;

        }

        return true;
    }

    @Override
    public boolean close() {

        try {
            con.close();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    @Override
    public boolean pre() {
        String globalStatsTable = "CREATE TABLE IF NOT EXISTS global_stats (" +
                "name TEXT," +
                " title TEXT," +
                " value TEXT," +
                "PRIMARY KEY (name));";
        String variant_stats = "CREATE TABLE IF NOT EXISTS variant_stats (" +
                "id_variant INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "chromosome TEXT, " +
                "position INT64, " +
                "allele_ref TEXT, " +
                "allele_alt TEXT, " +
                "maf DOUBLE, " +
                "mgf DOUBLE," +
                "allele_maf TEXT, " +
                "genotype_maf TEXT, " +
                "miss_allele INT, " +
                "miss_gt INT, " +
                "mendel_err INT, " +
                "is_indel INT, " +
                "cases_percent_dominant DOUBLE, " +
                "controls_percent_dominant DOUBLE, " +
                "cases_percent_recessive DOUBLE, " +
                "controls_percent_recessive DOUBLE);";
        String sample_stats = "CREATE TABLE IF NOT EXISTS sample_stats(" +
                "name TEXT, " +
                "mendelian_errors INT, " +
                "missing_genotypes INT, " +
                "homozygotesNumber INT, " +
                "PRIMARY KEY (name));";

        String variantTable = "CREATE TABLE IF NOT EXISTS variant (" +
                "id_variant INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "chromosome TEXT, " +
                "position INT64, " +
                "id TEXT, " +
                "ref TEXT, " +
                "alt TEXT, " +
                "qual DOUBLE, " +
                "filter TEXT, " +
                "info TEXT, " +
                "format TEXT);";

        String sampleTable = "CREATE TABLE IF NOT EXISTS sample(" +
                "name TEXT PRIMARY KEY);";

        String sampleInfoTable = "CREATE TABLE IF NOT EXISTS sample_info(" +
                "id_sample_info INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "id_variant INTEGER, " +
                "sample_name TEXT, " +
                "allele_1 INTEGER, " +
                "allele_2 INTEGER, " +
                "data TEXT, " +
                "FOREIGN KEY(id_variant) REFERENCES variant(id_variant)," +
                "FOREIGN KEY(sample_name) REFERENCES sample(name));";


        try {
            stmt = con.createStatement();

            stmt.execute(globalStatsTable);
            stmt.execute(variant_stats);
            stmt.execute(sample_stats);
            stmt.execute(variantTable);
            stmt.execute(sampleTable);
            stmt.execute(sampleInfoTable);

            stmt.close();

            con.commit();
        } catch (SQLException e) {
            System.err.println("PRE: " + e.getClass().getName() + ": " + e.getMessage());
            return false;
        }

        return true;
    }

    @Override
    public boolean post() {

        try {

            stmt = con.createStatement();
            stmt.execute("CREATE INDEX variant_stats_chromosome_position_idx ON variant_stats (chromosome, position);");
            stmt.execute("CREATE INDEX variant_chromosome_position_idx ON variant (chromosome, position);");
            stmt.execute("CREATE INDEX variant_pass_idx ON variant (filter);");
            stmt.execute("CREATE INDEX variant_id_idx ON variant (id);");
            stmt.execute("CREATE INDEX sample_name_idx ON sample (name);");
            stmt.close();
            con.commit();

        } catch (SQLException e) {
            System.err.println("POST: " + e.getClass().getName() + ": " + e.getMessage());
            return false;

        }

        return true;
    }

    @Override
    public boolean writeVariantStats(List<VcfRecordStat> data) {

        String sql = "INSERT INTO variant_stats (chromosome, position, allele_ref, allele_alt, maf, mgf, allele_maf, genotype_maf, miss_allele, miss_gt, mendel_err, is_indel, cases_percent_dominant, controls_percent_dominant, cases_percent_recessive, controls_percent_recessive) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?);";
        boolean res = true;


        try {
            pstmt = con.prepareStatement(sql);

            for (VcfRecordStat v : data) {
                pstmt.setString(1, v.getChromosome());
                pstmt.setLong(2, v.getPosition());
                pstmt.setString(3, v.getRefAlleles());
                pstmt.setString(4, StringUtils.join(v.getAltAlleles(), ","));
                pstmt.setDouble(5, v.getMaf());
                pstmt.setDouble(6, v.getMgf());
                pstmt.setString(7, v.getMafAllele());
                pstmt.setString(8, v.getMgfAllele());
                pstmt.setInt(9, v.getMissingAlleles());
                pstmt.setInt(10, v.getMissingGenotypes());
                pstmt.setInt(11, v.getMendelinanErrors());
                pstmt.setInt(12, (v.getIndel() ? 1 : 0));
                pstmt.setDouble(13, v.getCasesPercentDominant());
                pstmt.setDouble(14, v.getControlsPercentDominant());
                pstmt.setDouble(15, v.getCasesPercentRecessive());
                pstmt.setDouble(16, v.getControlsPercentRecessive());

                pstmt.execute();

            }
            con.commit();
            pstmt.close();
        } catch (SQLException e) {
            System.err.println("VARIANT_STATS: " + e.getClass().getName() + ": " + e.getMessage());
            res = false;
        }


        return res;
    }

    @Override
    public boolean writeGlobalStats(VcfGlobalStat globalStats) {
        boolean res = true;
        float titv = 0;
        float pass = 0;
        float avg = 0;
        try {
            String sql;
            stmt = con.createStatement();

            sql = "INSERT INTO global_stats VALUES ('NUM_VARIANTS', 'Number of variants'," + globalStats.getVariantsCount() + ");";
            stmt.executeUpdate(sql);
            sql = "INSERT INTO global_stats VALUES ('NUM_SAMPLES', 'Number of samples'," + globalStats.getSamplesCount() + ");";
            stmt.executeUpdate(sql);
            sql = "INSERT INTO global_stats VALUES ('NUM_BIALLELIC', 'Number of biallelic variants'," + globalStats.getBiallelicsCount() + ");";
            stmt.executeUpdate(sql);
            sql = "INSERT INTO global_stats VALUES ('NUM_MULTIALLELIC', 'Number of multiallelic variants'," + globalStats.getMultiallelicsCount() + ");";
            stmt.executeUpdate(sql);
            sql = "INSERT INTO global_stats VALUES ('NUM_SNPS', 'Number of SNP'," + globalStats.getSnpsCount() + ");";
            stmt.executeUpdate(sql);
            sql = "INSERT INTO global_stats VALUES ('NUM_INDELS', 'Number of indels'," + globalStats.getIndelsCount() + ");";
            stmt.executeUpdate(sql);
            sql = "INSERT INTO global_stats VALUES ('NUM_TRANSITIONS', 'Number of transitions'," + globalStats.getTransitionsCount() + ");";
            stmt.executeUpdate(sql);
            sql = "INSERT INTO global_stats VALUES ('NUM_TRANSVERSSIONS', 'Number of transversions'," + globalStats.getTransversionsCount() + ");";
            stmt.executeUpdate(sql);
            if(globalStats.getTransversionsCount() > 0){
                titv = globalStats.getTransitionsCount()/ (float) globalStats.getTransversionsCount();
            }
            sql = "INSERT INTO global_stats VALUES ('TITV_RATIO', 'Ti/TV ratio'," + titv + ");";
            stmt.executeUpdate(sql);
            if(globalStats.getVariantsCount() > 0){
                pass = globalStats.getPassCount()/ (float) globalStats.getVariantsCount();
                avg = globalStats.getAccumQuality()/(float) globalStats.getVariantsCount();
            }

            sql = "INSERT INTO global_stats VALUES ('PERCENT_PASS', 'Percentage of PASS'," + (pass * 100) + ");";
            stmt.executeUpdate(sql);

            sql = "INSERT INTO global_stats VALUES ('AVG_QUALITY', 'Average quality'," + avg + ");";
            stmt.executeUpdate(sql);

            con.commit();
            stmt.close();

        } catch (SQLException e) {
            System.err.println("GLOBAL_STATS: " + e.getClass().getName() + ": " + e.getMessage());
            res = false;
        }

        return res;
    }

    @Override
    public boolean writeSampleStats(VcfSampleStat sampleStat) {
        String sql = "INSERT INTO sample_stats VALUES(?,?,?,?);";
        SampleStat s;
        String name;
        boolean res = true;
        try {
            pstmt = con.prepareStatement(sql);

            for (Map.Entry<String, SampleStat> entry : sampleStat.getSamplesStats().entrySet()) {
                s = entry.getValue();
                name = entry.getKey();

                pstmt.setString(1, name);
                pstmt.setInt(2, s.getMendelianErrors());
                pstmt.setInt(3, s.getMissingGenotypes());
                pstmt.setInt(4, s.getHomozygotesNumber());

                pstmt.execute();

            }
            con.commit();
            pstmt.close();
        } catch (SQLException e) {
            System.err.println("SAMPLE_STATS: " + e.getClass().getName() + ": " + e.getMessage());
            res = false;
        }


        return res;
    }

    @Override
    public boolean writeSampleGroupStats(VcfSampleGroupStats sampleGroupStats) {
        return false;
    }

    @Override
    public boolean writeVariantGroupStats(VcfVariantGroupStat groupStats) {
        return false;
    }

    @Override
    public boolean writeVariantIndex(List<VcfRecord> data) {
        String sql, sqlAux;
        PreparedStatement pstmt_aux;
        String sampleName;
        String sampleData;
        int allele_1, allele_2;
        Genotype g;
        int id;
        boolean res = true;

        PreparedStatement pstmt;
        if (!createdSampleTable && data.size() > 0) {
            try {
                sql = "INSERT INTO sample (name) VALUES(?);";
                pstmt = con.prepareStatement(sql);
                VcfRecord v = data.get(0);
                for (Map.Entry<String, Integer> entry : v.getSampleIndex().entrySet()) {
                    pstmt.setString(1, entry.getKey());
                    pstmt.execute();

                }

                pstmt.close();
                con.commit();
                createdSampleTable = true;
            } catch (SQLException e) {
                System.err.println("SAMPLE: " + e.getClass().getName() + ": " + e.getMessage());
                res = false;
            }
        }

        sql = "INSERT INTO variant (chromosome, position, id, ref, alt, qual, filter, info, format) VALUES(?,?,?,?,?,?,?,?,?);";
        sqlAux = "INSERT INTO sample_info(id_variant, sample_name, allele_1, allele_2, data) VALUES (?,?,?,?,?);";
        try {

            pstmt = con.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS);
            pstmt_aux = con.prepareStatement(sqlAux);

            for (VcfRecord v : data) {

                pstmt.setString(1, v.getChromosome());
                pstmt.setInt(2, v.getPosition());
                pstmt.setString(3, v.getId());
                pstmt.setString(4, v.getReference());
                pstmt.setString(5, StringUtils.join(v.getAltAlleles(), ","));
                pstmt.setDouble(6, (v.getQuality().equals(".") ? 0 : Double.valueOf(v.getQuality())));
                pstmt.setString(7, v.getFilter());
                pstmt.setString(8, v.getInfo());
                pstmt.setString(9, v.getFormat());

                pstmt.execute();
                ResultSet rs = pstmt.getGeneratedKeys();
                if (rs.next()) {
                    id = rs.getInt(1);
                    for (Map.Entry<String, Integer> entry : v.getSampleIndex().entrySet()) {
                        sampleName = entry.getKey();
                        sampleData = v.getSamples().get(entry.getValue());
                        g = v.getSampleGenotype(sampleName);

                        allele_1 = (g.getAllele1() == null) ? -1 : g.getAllele1();
                        allele_2 = (g.getAllele2() == null) ? -1 : g.getAllele2();

                        pstmt_aux.setInt(1, id);
                        pstmt_aux.setString(2, sampleName);
                        pstmt_aux.setInt(3, allele_1);
                        pstmt_aux.setInt(4, allele_2);
                        pstmt_aux.setString(5, sampleData);
                        pstmt_aux.execute();

                    }

                } else {
                    res = false;
                }


            }
            pstmt.close();
            pstmt_aux.close();

            con.commit();
        } catch (SQLException e) {
            System.err.println("VARIANT/SAMPLE_INFO: " + e.getClass().getName() + ": " + e.getMessage());
            res = false;
        }

        return res;
    }

}
