package com.andisec.comparison.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DataDiff {

    private String th1;

    private String th2;

    private String diff;


}
