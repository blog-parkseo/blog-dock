package com.blogdock.blog;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "reserved_word")
public class ReservedWord {

    @Id
    private String word;

    protected ReservedWord() {
    }
}
