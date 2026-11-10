package com.campusmart.common;

import lombok.Data;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

@Data
public class PageResult<T> implements Serializable {
    private static final long serialVersionUID = 1L;

    private long total;
    private long pages;
    private long current;
    private long size;
    private List<T> records;

    public static <T> PageResult<T> of(long total, long pages, long current,
                                       long size, List<T> records) {
        PageResult<T> pr = new PageResult<>();
        pr.setTotal(total);
        pr.setPages(pages);
        pr.setCurrent(current);
        pr.setSize(size);
        pr.setRecords(records == null ? Collections.emptyList() : records);
        return pr;
    }

    public static <T> PageResult<T> empty() {
        return of(0, 0, 1, 10, Collections.emptyList());
    }
}
