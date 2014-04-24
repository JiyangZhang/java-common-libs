package org.opencb.commons.bioformats.variant.vcf4.io.readers;

import org.opencb.commons.bioformats.feature.Genotype;
import org.opencb.commons.bioformats.feature.Genotypes;
import org.opencb.commons.bioformats.variant.Variant;
import org.opencb.commons.bioformats.variant.utils.stats.VariantStats;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Alejandro Aleman Ramos <aaleman@cipf.es>
 */
public class VariantVcfEVSReader extends VariantVcfReader implements VariantReader {

    private Pattern singleNuc = Pattern.compile("^[ACTG]$");
    private Pattern singleRef = Pattern.compile("^R$");
    private Pattern refAlt = Pattern.compile("^([ACTG])([ACTG])$");
    private Pattern refRef = Pattern.compile("^R{2}$");
    private Pattern altNum = Pattern.compile("^A(\\d+)$");
    private Pattern altNumaltNum = Pattern.compile("^A(\\d+)A(\\d+)$");
    private Pattern altNumRef = Pattern.compile("^A(\\d+)R$");

    public VariantVcfEVSReader(String filename) {
        super(filename);
    }

    @Override
    public Variant read() {
        Variant variant = super.read();

        if (variant != null) {
            VariantStats stats = new VariantStats();
            stats.setChromosome(variant.getChromosome());
            stats.setPosition(variant.getPosition());
            stats.setRefAllele(variant.getReference());
            stats.setAltAlleles(variant.getAltAlleles());
            if (variant.containsAttribute("MAF")) {
                String splitsMaf[] = variant.getAttribute("MAF").split(",");
                if (splitsMaf.length == 3) {
                    float maf = Float.parseFloat(splitsMaf[2]) / 100;
                    stats.setMaf(maf);
                }
            }

            if (variant.containsAttribute("GTS") && variant.containsAttribute("GTC")) {
                String splitsGTS[] = variant.getAttribute("GTS").split(",");
                String splitsGTC[] = variant.getAttribute("GTC").split(",");

                List<Genotype> genotypeList = new ArrayList<>();

                if (splitsGTC.length == splitsGTS.length) {

                    for (int i = 0; i < splitsGTC.length; i++) {
                        String gt = splitsGTS[i];
                        int gtCount = Integer.parseInt(splitsGTC[i]);

                        Genotype g = parseGenotype(gt, variant);

                        if (g != null) {
                            g.setCount(gtCount);
                            Genotypes.addGenotypeToList(genotypeList, g);
                        }
                    }
                    stats.setMafAllele("");
                    stats.setMissingAlleles(0);
                    stats.setGenotypes(genotypeList);
                }
            }
            variant.setStats(stats);
        }
        return variant;
    }

    private Genotype parseGenotype(String gt, Variant variant) {
        Genotype g;
        Matcher m;

        m = singleNuc.matcher(gt);

        if (m.matches()) { // A,C,T,G
            g = new Genotype(gt + "/" + gt, variant.getReference(), variant.getAlternate());
            return g;
        }
        m = singleRef.matcher(gt);
        if (m.matches()) { // R
            g = new Genotype(variant.getReference() + "/" + variant.getReference(), variant.getReference(), variant.getAlternate());
            return g;
        }

        m = refAlt.matcher(gt);
        if (m.matches()) { // AA,AC,TT,GT,...
            String ref = m.group(1);
            String alt = m.group(2);
            g = new Genotype(ref + "/" + alt, variant.getReference(), variant.getAlternate());
            return g;
        }

        m = refRef.matcher(gt);
        if (m.matches()) { // RR
            g = new Genotype(variant.getReference() + "/" + variant.getReference(), variant.getReference(), variant.getAlternate());
            return g;
        }

        m = altNum.matcher(gt);
        if (m.matches()) { // A1,A2,A3
            int val = Integer.parseInt(m.group(1));
            String altN = variant.getAltAlleles()[val - 1];
            g = new Genotype(altN + "/" + altN, variant.getReference(), variant.getAlternate());
            return g;
        }

        m = altNumaltNum.matcher(gt);
        if (m.matches()) { // A1A2,A1A3...
            int val1 = Integer.parseInt(m.group(1));
            String altN1 = variant.getAltAlleles()[val1 - 1];

            int val2 = Integer.parseInt(m.group(2));
            String altN2 = variant.getAltAlleles()[val2 - 1];

            g = new Genotype(altN1 + "/" + altN2, variant.getReference(), variant.getAlternate());
            return g;
        }

        m = altNumRef.matcher(gt);
        if (m.matches()) {
            int val1 = Integer.parseInt(m.group(1));
            String altN1 = variant.getAltAlleles()[val1 - 1];

            g = new Genotype(altN1 + "/" + variant.getReference(), variant.getReference(), variant.getAlternate());
            return g;
        }

        return null;
    }
}
