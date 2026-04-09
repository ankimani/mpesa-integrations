package com.mpesa.integration.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TransactionPageData {
    private List<TransactionListItemData> items;
    private int page;
    private int size;
    private boolean hasNext;
}
