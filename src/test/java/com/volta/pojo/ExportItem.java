package com.volta.pojo;

import lombok.Data;
import java.util.List;

@Data
public class ExportItem {

    private int id;
    List<VerifyDataItem> verifyData;

}
