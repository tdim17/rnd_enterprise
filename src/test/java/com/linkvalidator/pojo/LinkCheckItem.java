package com.linkvalidator.pojo;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode
public class LinkCheckItem {
    private int id;
    private String type;
    private String link;

}
