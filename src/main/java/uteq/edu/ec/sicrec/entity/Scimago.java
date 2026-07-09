package uteq.edu.ec.sicrec.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "scimago")
public class Scimago {
    @Id
    @Column(name = "\"Sourceid\"")
    private Double sourceid;

    @Column(name = "\"Rank\"")
    private Double rank;

    @Column(name = "\"Title\"", length = Integer.MAX_VALUE)
    private String title;

    @Column(name = "\"Type\"", length = Integer.MAX_VALUE)
    private String type;

    @Column(name = "\"Issn\"", length = Integer.MAX_VALUE)
    private String issn;

    @Column(name = "\"Publisher...6\"", length = Integer.MAX_VALUE)
    private String publisher6;

    @Column(name = "\"Open Access\"", length = Integer.MAX_VALUE)
    private String openAccess;

    @Column(name = "\"Open Access Diamond\"", length = Integer.MAX_VALUE)
    private String openAccessDiamond;

    @Column(name = "\"SJR\"", length = Integer.MAX_VALUE)
    private String sjr;

    @Column(name = "\"SJR Best Quartile\"", length = Integer.MAX_VALUE)
    private String sJRBestQuartile;

    @Column(name = "\"H index\"")
    private Double hIndex;

    @Column(name = "\"Total Docs. (2025)\"")
    private Double totalDocs2025;

    @Column(name = "\"Total Docs. (3years)\"")
    private Double totalDocs3years;

    @Column(name = "\"Total Refs.\"")
    private Double totalRefs;

    @Column(name = "\"Total Citations (3years)\"")
    private Double totalCitations3years;

    @Column(name = "\"Citable Docs. (3years)\"")
    private Double citableDocs3years;

    @Column(name = "\"Citations / Doc. (2years)\"", length = Integer.MAX_VALUE)
    private String citationsDoc2years;

    @Column(name = "\"Ref. / Doc.\"", length = Integer.MAX_VALUE)
    private String refDoc;

    @Column(name = "\"%Female\"", length = Integer.MAX_VALUE)
    private String Female;

    @Column(name = "\"Overton\"")
    private Double overton;

    @Column(name = "\"Country\"", length = Integer.MAX_VALUE)
    private String country;

    @Column(name = "\"Region\"", length = Integer.MAX_VALUE)
    private String region;

    @Column(name = "\"Publisher...23\"", length = Integer.MAX_VALUE)
    private String publisher23;

    @Column(name = "\"Coverage\"", length = Integer.MAX_VALUE)
    private String coverage;

    @Column(name = "\"Categories\"", length = Integer.MAX_VALUE)
    private String categories;

    @Column(name = "\"Areas\"", length = Integer.MAX_VALUE)
    private String areas;


}