package dev.vality.fraudbusters.dgraph.service.query;

import dev.vality.fraudbusters.config.dgraph.TemplateConfig;
import dev.vality.fraudbusters.fraud.payment.aggregator.dgraph.query.builder.DgraphCountQueryBuilderService;
import dev.vality.fraudbusters.fraud.payment.aggregator.dgraph.query.builder.DgraphSumQueryBuilderService;
import dev.vality.fraudbusters.fraud.payment.aggregator.dgraph.query.builder.DgraphUniqueQueryBuilderService;
import dev.vality.fraudbusters.fraud.payment.resolver.DgraphEntityResolver;
import dev.vality.fraudbusters.fraud.payment.resolver.DgraphQueryConditionResolver;
import dev.vality.fraudbusters.service.template.aggregate.count.CountQueryTemplateService;
import dev.vality.fraudbusters.service.template.aggregate.count.RootCountQueryTemplateService;
import dev.vality.fraudbusters.service.template.aggregate.sum.RootSumQueryTemplateService;
import dev.vality.fraudbusters.service.template.aggregate.sum.SumQueryTemplateService;
import dev.vality.fraudbusters.service.template.aggregate.unique.EqualFieldsUniqueQueryTemplateService;
import dev.vality.fraudbusters.service.template.aggregate.unique.RootUniqueQueryTemplateService;
import dev.vality.fraudbusters.service.template.aggregate.unique.UniqueQueryTemplateService;
import org.apache.velocity.app.VelocityEngine;

public abstract class AbstractDgraphQueryBuilderServiceTest {

    private VelocityEngine velocityEngine = new TemplateConfig().velocityEngine();
    private DgraphEntityResolver dgraphEntityResolver = new DgraphEntityResolver();
    private DgraphQueryConditionResolver dgraphQueryConditionResolver = new DgraphQueryConditionResolver();

    DgraphCountQueryBuilderService countQueryBuilderService = new DgraphCountQueryBuilderService(
            dgraphEntityResolver,
            dgraphQueryConditionResolver,
            new CountQueryTemplateService(velocityEngine),
            new RootCountQueryTemplateService(velocityEngine)
    );

    DgraphSumQueryBuilderService sumQueryBuilderService = new DgraphSumQueryBuilderService(
            dgraphEntityResolver,
            dgraphQueryConditionResolver,
            new SumQueryTemplateService(velocityEngine),
            new RootSumQueryTemplateService(velocityEngine)
    );

    DgraphUniqueQueryBuilderService uniqueQueryBuilderService = new DgraphUniqueQueryBuilderService(
            dgraphEntityResolver,
            dgraphQueryConditionResolver,
            new UniqueQueryTemplateService(velocityEngine),
            new RootUniqueQueryTemplateService(velocityEngine),
            new EqualFieldsUniqueQueryTemplateService(velocityEngine)
    );

}
