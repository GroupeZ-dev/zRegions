package fr.maxlego08.zregions.common.importer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Outcome of an import run. Counters feed the chat summary; every skipped
 * region/flag/member gets a human-readable detail line for the server log —
 * nothing is ever dropped silently (plan §15).
 */
public final class ImportReport {

    private final boolean dryRun;
    private int regionsImported;
    private int regionsSkipped;
    private int flagsApplied;
    private int flagsSkipped;
    private int membersImported;
    private int membersSkipped;
    private final List<String> details = new ArrayList<>();

    public ImportReport(boolean dryRun) {
        this.dryRun = dryRun;
    }

    public boolean isDryRun() {
        return this.dryRun;
    }

    void regionImported() {
        this.regionsImported++;
    }

    void regionSkipped(String detail) {
        this.regionsSkipped++;
        this.details.add(detail);
    }

    void flagApplied() {
        this.flagsApplied++;
    }

    void flagSkipped(String detail) {
        this.flagsSkipped++;
        this.details.add(detail);
    }

    void memberImported() {
        this.membersImported++;
    }

    void memberSkipped(String detail) {
        this.membersSkipped++;
        this.details.add(detail);
    }

    void error(String detail) {
        this.details.add(detail);
    }

    public int getRegionsImported() {
        return this.regionsImported;
    }

    public int getRegionsSkipped() {
        return this.regionsSkipped;
    }

    public int getFlagsApplied() {
        return this.flagsApplied;
    }

    public int getFlagsSkipped() {
        return this.flagsSkipped;
    }

    public int getMembersImported() {
        return this.membersImported;
    }

    public int getMembersSkipped() {
        return this.membersSkipped;
    }

    /** Every skip/error line, in occurrence order — destined for the server log. */
    public List<String> getDetails() {
        return Collections.unmodifiableList(this.details);
    }
}
