package com.srms.api.modules.library.entity;

import com.srms.api.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "library_books", indexes = @Index(name = "idx_library_books_school_id", columnList = "school_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LibraryBook extends BaseEntity {

    public enum Status { AVAILABLE, ALL_BORROWED }

    @Column(nullable = false)
    private String schoolId;

    @Column(unique = true)
    private String isbn;

    @Column(nullable = false)
    private String title;

    private String author;
    private String category;
    private String publisher;
    private Integer yearPublished;
    private int totalCopies;
    private int availableCopies;

    @Column(name = "shelf_location")
    private String location;

    @Enumerated(EnumType.STRING)
    private Status status = Status.AVAILABLE;
}
