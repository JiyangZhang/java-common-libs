package org.opencb.commons.bioformats.commons.core.connectors.variant;

import com.google.common.base.Predicate;
import org.bioinfo.commons.io.utils.FileUtils;
import org.bioinfo.commons.utils.StringUtils;
import org.opencb.commons.bioformats.commons.core.variant.Vcf4;
import org.opencb.commons.bioformats.commons.core.variant.vcf4.VcfFilter;
import org.opencb.commons.bioformats.commons.core.variant.vcf4.VcfFormat;
import org.opencb.commons.bioformats.commons.core.variant.vcf4.VcfInfo;
import org.opencb.commons.bioformats.commons.core.variant.vcf4.VcfRecord;
import org.opencb.commons.bioformats.commons.exception.FileFormatException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: aaleman
 * Date: 8/30/13
 * Time: 12:24 PM
 * To change this template use File | Settings | File Templates.
 */
public class VcfFileDataReader implements VcfDataReader {

    private static final int DEFAULT_NUMBER_RECORDS = 40000;
    private Vcf4 vcf4;
    private BufferedReader reader;
    private List<Predicate<VcfRecord>> vcfFilters;
    private Predicate<VcfRecord> andVcfFilters;
    private File file;
    private String filename;

    public VcfFileDataReader(String filename) {
        this.filename = filename;
    }


    @Override
    public boolean open() throws IOException {
        this.file = new File(this.filename);
        vcf4 = new Vcf4();

        FileUtils.checkFile(this.file);
        this.reader = new BufferedReader(new FileReader(this.file));

        return true;
    }


    @Override
    public boolean pre() throws IOException, FileFormatException {

        processHeader();

        return true;
    }

    @Override
    public boolean close() throws IOException {
        reader.close();
        return true;
    }


    @Override
    public boolean post() {
        return true;
    }

    @Override
    public VcfRecord read() {
        String line;
        try {
            while ((line = reader.readLine()) != null && (line.trim().equals("") || line.startsWith("#"))) {
                ;
            }
            if (line != null) {
                String[] fields = line.split("\t");
                VcfRecord vcfRecord = null;
                if (fields.length == 8) {
                    vcfRecord = new VcfRecord(fields[0], Integer.parseInt(fields[1]), fields[2], fields[3], fields[4], fields[5], fields[6], fields[7]);
                    vcfRecord.setSampleIndex(vcf4.getSamples());
                } else {
                    if (fields.length > 8) {
                        vcfRecord = new VcfRecord(fields);
                        vcfRecord.setSampleIndex(vcf4.getSamples());
                    }
                }
                return vcfRecord;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }


        return null;
    }

    @Override
    public List<VcfRecord> read(int batchSize) {
        List<VcfRecord> listRecords = new ArrayList<>(batchSize);
        VcfRecord vcfRecord;
        int i = 0;

        while ((i < batchSize) && (vcfRecord = this.read()) != null) {

            if (vcfFilters != null && vcfFilters.size() > 0) {
                if (andVcfFilters.apply(vcfRecord)) {
                    vcfRecord.setSampleIndex(vcf4.getSamples());
                    listRecords.add(vcfRecord);
                    i++;
                }
            } else {
                vcfRecord.setSampleIndex(vcf4.getSamples());
                listRecords.add(vcfRecord);
                i++;
            }

        }
        return listRecords;
    }


    @Override
    public List<String> getSampleNames() {
        return this.vcf4.getSampleNames();
    }


    private void processHeader() throws IOException, FileFormatException {
        VcfInfo vcfInfo;
        VcfFilter vcfFilter;
        VcfFormat vcfFormat;
        List<String> headerLine;
        String line;
        String[] fields;
        BufferedReader localBufferedReader = new BufferedReader(new FileReader(file));
        while ((line = localBufferedReader.readLine()) != null && line.startsWith("#")) {

            if (line.startsWith("##fileformat")) {
                if (line.split("=").length > 1) {

                    vcf4.setFileFormat(line.split("=")[1].trim());
                } else {
                    throw new FileFormatException("");
                }
            } else if (line.startsWith("##INFO")) {

                vcfInfo = new VcfInfo(line);
                vcf4.getInfo().put(vcfInfo.getId(), vcfInfo);
            } else if (line.startsWith("##FILTER")) {

                vcfFilter = new VcfFilter(line);
                vcf4.getFilter().put(vcfFilter.getId(), vcfFilter);
            } else if (line.startsWith("##FORMAT")) {

                vcfFormat = new VcfFormat(line);
                vcf4.getFormat().put(vcfFormat.getId(), vcfFormat);
            } else if (line.startsWith("#CHROM")) {
                headerLine = StringUtils.toList(line.replace("#", ""), "\t");

                vcf4.setHeaderLine(headerLine);
            } else {
                fields = line.replace("#", "").split("=", 2);
                vcf4.getMetaInformation().put(fields[0], fields[1]);
            }
        }
        localBufferedReader.close();
    }
}
