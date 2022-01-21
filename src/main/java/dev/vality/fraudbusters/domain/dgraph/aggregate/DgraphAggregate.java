package dev.vality.fraudbusters.domain.dgraph.aggregate;

import lombok.Data;

@Data
public class DgraphAggregate {

    private int count;
    private long sum;

}
