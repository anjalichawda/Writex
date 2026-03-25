package com.ebook.backend.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "chapters")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Chapter {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;
    private String coverUrl;

    @Lob
    @Column(columnDefinition = "LONGBLOB")
    @Basic(fetch = FetchType.LAZY)
    @JsonIgnore
    private byte[] coverImageData;

    @JsonIgnore
    private String coverImageContentType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id")
    @JsonBackReference
    private Book book;

    private Integer orderIndex;
}
