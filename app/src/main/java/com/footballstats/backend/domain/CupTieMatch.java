package com.footballstats.backend.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "w_cup_tie_match", schema = "work")
public class CupTieMatch {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "tie_id", nullable = false) private CupTie tie;
    @OneToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "match_id", nullable = false, unique = true) private TourMatch match;
    @Column(name = "leg_number", nullable = false) private int legNumber;
    public Long getId() { return id; }
    public CupTie getTie() { return tie; }
    public void setTie(CupTie value) { tie = value; }
    public TourMatch getMatch() { return match; }
    public void setMatch(TourMatch value) { match = value; }
    public int getLegNumber() { return legNumber; }
    public void setLegNumber(int value) { legNumber = value; }
}
