package com.andisec.comparison.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class FileInfo {
    private String path;
    private boolean isDirectory;
    private String md5;

}
