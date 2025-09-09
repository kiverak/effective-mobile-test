package com.example.bankcards.util;

import org.springframework.data.domain.Sort;

public class SortingUtils {

    public static Sort getSort(String sort) {
        Sort sortSpec = Sort.unsorted();
        if (sort != null && !sort.isBlank()) {
            Sort.Direction direction = Sort.Direction.fromString(sort);
            sortSpec = Sort.by(direction, "id");
        }
        return sortSpec;
    }
}
