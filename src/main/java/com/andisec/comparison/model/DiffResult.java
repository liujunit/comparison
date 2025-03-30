package com.andisec.comparison.model;

import lombok.Data;

import java.util.List;

@Data
public class DiffResult {

    private List<DataDiff> dataDiffs;

    private Integer num1;

    private Integer num2;

}
