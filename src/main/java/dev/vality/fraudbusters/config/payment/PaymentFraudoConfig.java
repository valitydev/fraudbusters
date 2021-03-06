package dev.vality.fraudbusters.config.payment;

import dev.vality.damsel.wb_list.WbListServiceSrv;
import dev.vality.fraudbusters.fraud.constant.PaymentCheckedField;
import dev.vality.fraudbusters.fraud.localstorage.LocalResultStorageRepository;
import dev.vality.fraudbusters.fraud.localstorage.aggregator.LocalCountAggregatorDecorator;
import dev.vality.fraudbusters.fraud.localstorage.aggregator.LocalSumAggregatorDecorator;
import dev.vality.fraudbusters.fraud.localstorage.aggregator.LocalUniqueValueAggregatorDecorator;
import dev.vality.fraudbusters.fraud.model.PaymentModel;
import dev.vality.fraudbusters.fraud.payment.CountryByIpResolver;
import dev.vality.fraudbusters.fraud.payment.aggregator.clickhouse.CountAggregatorImpl;
import dev.vality.fraudbusters.fraud.payment.aggregator.clickhouse.SumAggregatorImpl;
import dev.vality.fraudbusters.fraud.payment.aggregator.clickhouse.UniqueValueAggregatorImpl;
import dev.vality.fraudbusters.fraud.payment.aggregator.dgraph.DgraphCountAggregatorImpl;
import dev.vality.fraudbusters.fraud.payment.aggregator.dgraph.DgraphSumAggregatorImpl;
import dev.vality.fraudbusters.fraud.payment.aggregator.dgraph.DgraphUniqueAggregatorImpl;
import dev.vality.fraudbusters.fraud.payment.aggregator.dgraph.query.builder.DgraphAggregationQueryBuilderService;
import dev.vality.fraudbusters.fraud.payment.finder.PaymentInListFinderImpl;
import dev.vality.fraudbusters.fraud.payment.resolver.CountryResolverImpl;
import dev.vality.fraudbusters.fraud.payment.resolver.DatabasePaymentFieldResolver;
import dev.vality.fraudbusters.fraud.payment.resolver.DgraphEntityResolver;
import dev.vality.fraudbusters.fraud.payment.resolver.PaymentModelFieldResolver;
import dev.vality.fraudbusters.repository.DgraphAggregatesRepository;
import dev.vality.fraudbusters.repository.PaymentRepository;
import dev.vality.fraudbusters.repository.clickhouse.impl.ChargebackRepository;
import dev.vality.fraudbusters.repository.clickhouse.impl.RefundRepository;
import dev.vality.fraudbusters.service.TimeBoundaryService;
import dev.vality.fraudo.aggregator.UniqueValueAggregator;
import dev.vality.fraudo.bundle.AggregatorBundle;
import dev.vality.fraudo.bundle.FinderBundle;
import dev.vality.fraudo.bundle.ResolverBundle;
import dev.vality.fraudo.bundle.VisitorBundle;
import dev.vality.fraudo.finder.InListFinder;
import dev.vality.fraudo.payment.aggregator.CountPaymentAggregator;
import dev.vality.fraudo.payment.aggregator.SumPaymentAggregator;
import dev.vality.fraudo.payment.factory.FraudVisitorFactoryImpl;
import dev.vality.fraudo.payment.factory.FullVisitorFactoryImpl;
import dev.vality.fraudo.payment.resolver.CustomerTypeResolver;
import dev.vality.fraudo.payment.resolver.PaymentGroupResolver;
import dev.vality.fraudo.payment.resolver.PaymentTimeWindowResolver;
import dev.vality.fraudo.payment.resolver.PaymentTypeResolver;
import dev.vality.fraudo.payment.visitor.impl.FirstFindVisitorImpl;
import dev.vality.fraudo.resolver.CountryResolver;
import dev.vality.fraudo.resolver.FieldResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

@Configuration
public class PaymentFraudoConfig {

    @Bean
    public CountPaymentAggregator<PaymentModel, PaymentCheckedField> countAggregator(
            PaymentRepository paymentRepository,
            RefundRepository refundRepository,
            ChargebackRepository chargebackRepository,
            DatabasePaymentFieldResolver databasePaymentFieldResolver,
            TimeBoundaryService timeBoundaryService) {
        return new CountAggregatorImpl(
                databasePaymentFieldResolver,
                paymentRepository,
                refundRepository,
                chargebackRepository,
                timeBoundaryService
        );
    }

    @Bean
    public SumPaymentAggregator<PaymentModel, PaymentCheckedField> sumAggregator(
            PaymentRepository paymentRepository,
            RefundRepository refundRepository,
            ChargebackRepository chargebackRepository,
            DatabasePaymentFieldResolver databasePaymentFieldResolver,
            TimeBoundaryService timeBoundaryService) {
        return new SumAggregatorImpl(
                databasePaymentFieldResolver,
                paymentRepository,
                refundRepository,
                chargebackRepository,
                timeBoundaryService
        );
    }

    @Bean
    public UniqueValueAggregator<PaymentModel, PaymentCheckedField> uniqueValueAggregator(
            PaymentRepository paymentRepository,
            DatabasePaymentFieldResolver databasePaymentFieldResolver,
            TimeBoundaryService timeBoundaryService) {
        return new UniqueValueAggregatorImpl(databasePaymentFieldResolver, paymentRepository, timeBoundaryService);
    }

    @Bean
    public CountryResolver<PaymentCheckedField> countryResolver(CountryByIpResolver countryByIpResolver) {
        return new CountryResolverImpl(countryByIpResolver);
    }

    @Bean
    public FieldResolver<PaymentModel, PaymentCheckedField> paymentModelFieldResolver() {
        return new PaymentModelFieldResolver();
    }

    @Bean
    public InListFinder<PaymentModel, PaymentCheckedField> paymentInListFinder(
            WbListServiceSrv.Iface wbListServiceSrv,
            PaymentRepository paymentRepository,
            DatabasePaymentFieldResolver databasePaymentFieldResolver) {
        return new PaymentInListFinderImpl(wbListServiceSrv, databasePaymentFieldResolver, paymentRepository);
    }

    @Bean
    public FirstFindVisitorImpl<PaymentModel, PaymentCheckedField> paymentRuleVisitor(
            CountPaymentAggregator<PaymentModel, PaymentCheckedField> countAggregator,
            SumPaymentAggregator<PaymentModel, PaymentCheckedField> sumAggregator,
            UniqueValueAggregator<PaymentModel, PaymentCheckedField> uniqueValueAggregator,
            CountryResolver<PaymentCheckedField> countryResolver,
            InListFinder<PaymentModel, PaymentCheckedField> paymentInListFinder,
            FieldResolver<PaymentModel, PaymentCheckedField> paymentModelFieldResolver,
            PaymentTypeResolver<PaymentModel> paymentTypeResolver,
            CustomerTypeResolver<PaymentModel> customerTypeResolver) {
        AggregatorBundle<PaymentModel, PaymentCheckedField> aggregatorBundle =
                new AggregatorBundle<>(countAggregator, sumAggregator, uniqueValueAggregator);
        ResolverBundle<PaymentModel, PaymentCheckedField> resolverBundle = new ResolverBundle<>(
                countryResolver,
                paymentModelFieldResolver,
                new PaymentGroupResolver<>(paymentModelFieldResolver),
                new PaymentTimeWindowResolver(),
                paymentTypeResolver,
                customerTypeResolver
        );
        FinderBundle<PaymentModel, PaymentCheckedField> finderBundle = new FinderBundle<>(paymentInListFinder);
        return new FraudVisitorFactoryImpl()
                .createVisitor(new VisitorBundle<>(aggregatorBundle, resolverBundle, finderBundle));
    }

    @Bean
    public CountPaymentAggregator<PaymentModel, PaymentCheckedField> countResultAggregator(
            LocalResultStorageRepository localResultStorageRepository,
            PaymentRepository paymentRepositoryImpl,
            RefundRepository refundRepository,
            ChargebackRepository chargebackRepository,
            DatabasePaymentFieldResolver databasePaymentFieldResolver,
            TimeBoundaryService timeBoundaryService) {

        CountAggregatorImpl countAggregatorDecorator = new CountAggregatorImpl(
                databasePaymentFieldResolver,
                paymentRepositoryImpl,
                refundRepository,
                chargebackRepository,
                timeBoundaryService
        );
        return new LocalCountAggregatorDecorator(
                countAggregatorDecorator,
                databasePaymentFieldResolver,
                localResultStorageRepository,
                timeBoundaryService
        );
    }

    @Bean
    public SumPaymentAggregator<PaymentModel, PaymentCheckedField> sumResultAggregator(
            LocalResultStorageRepository localResultStorageRepository,
            PaymentRepository paymentRepositoryImpl,
            RefundRepository refundRepository,
            ChargebackRepository chargebackRepository,
            DatabasePaymentFieldResolver databasePaymentFieldResolver,
            TimeBoundaryService timeBoundaryService) {

        SumAggregatorImpl sumAggregator = new SumAggregatorImpl(
                databasePaymentFieldResolver,
                paymentRepositoryImpl,
                refundRepository,
                chargebackRepository,
                timeBoundaryService
        );
        return new LocalSumAggregatorDecorator(
                sumAggregator,
                databasePaymentFieldResolver,
                localResultStorageRepository,
                timeBoundaryService
        );
    }

    @Bean
    public UniqueValueAggregator<PaymentModel, PaymentCheckedField> uniqueValueResultAggregator(
            LocalResultStorageRepository localResultStorageRepository,
            PaymentRepository fraudResultRepository,
            DatabasePaymentFieldResolver databasePaymentFieldResolver,
            TimeBoundaryService timeBoundaryService) {
        UniqueValueAggregatorImpl uniqueValueAggregator =
                new UniqueValueAggregatorImpl(databasePaymentFieldResolver, fraudResultRepository, timeBoundaryService);

        return new LocalUniqueValueAggregatorDecorator(
                uniqueValueAggregator,
                databasePaymentFieldResolver,
                localResultStorageRepository,
                timeBoundaryService
        );
    }

    @Bean
    public FirstFindVisitorImpl<PaymentModel, PaymentCheckedField> fullPaymentRuleVisitor(
            CountPaymentAggregator<PaymentModel, PaymentCheckedField> countResultAggregator,
            SumPaymentAggregator<PaymentModel, PaymentCheckedField> sumResultAggregator,
            UniqueValueAggregator<PaymentModel, PaymentCheckedField> uniqueValueResultAggregator,
            CountryResolver<PaymentCheckedField> countryResolver,
            InListFinder<PaymentModel, PaymentCheckedField> paymentInListFinder,
            FieldResolver<PaymentModel, PaymentCheckedField> paymentModelFieldResolver,
            PaymentTypeResolver<PaymentModel> paymentTypeResolver,
            CustomerTypeResolver<PaymentModel> customerTypeResolver) {
        AggregatorBundle<PaymentModel, PaymentCheckedField> aggregatorBundle =
                new AggregatorBundle<>(countResultAggregator, sumResultAggregator, uniqueValueResultAggregator);
        ResolverBundle<PaymentModel, PaymentCheckedField> resolverBundle = new ResolverBundle<>(
                countryResolver,
                paymentModelFieldResolver,
                new PaymentGroupResolver<>(paymentModelFieldResolver),
                new PaymentTimeWindowResolver(),
                paymentTypeResolver,
                customerTypeResolver
        );
        FinderBundle<PaymentModel, PaymentCheckedField> finderBundle = new FinderBundle<>(paymentInListFinder);
        return new FullVisitorFactoryImpl()
                .createVisitor(new VisitorBundle<>(aggregatorBundle, resolverBundle, finderBundle));
    }

    @Bean
    @Lazy
    public UniqueValueAggregator<PaymentModel, PaymentCheckedField> dgraphUniqueAggregator(
            DgraphAggregationQueryBuilderService dgraphUniqueQueryBuilderService,
            DgraphEntityResolver dgraphEntityResolver,
            DgraphAggregatesRepository dgraphAggregatesRepository,
            DatabasePaymentFieldResolver databasePaymentFieldResolver,
            TimeBoundaryService timeBoundaryService) {
        return new DgraphUniqueAggregatorImpl(
                dgraphUniqueQueryBuilderService,
                dgraphEntityResolver,
                dgraphAggregatesRepository,
                databasePaymentFieldResolver,
                timeBoundaryService
        );
    }

    @Bean
    @Lazy
    public CountPaymentAggregator<PaymentModel, PaymentCheckedField> dgraphCountAggregator(
            DgraphAggregationQueryBuilderService dgraphCountQueryBuilderService,
            DgraphEntityResolver dgraphEntityResolver,
            DgraphAggregatesRepository dgraphAggregatesRepository,
            TimeBoundaryService timeBoundaryService
    ) {
        return new DgraphCountAggregatorImpl(
                dgraphCountQueryBuilderService,
                dgraphEntityResolver,
                dgraphAggregatesRepository,
                timeBoundaryService
        );
    }

    @Bean
    @Lazy
    public SumPaymentAggregator<PaymentModel, PaymentCheckedField> dgraphSumAggregator(
            DgraphAggregationQueryBuilderService dgraphSumQueryBuilderService,
            DgraphEntityResolver dgraphEntityResolver,
            DgraphAggregatesRepository dgraphAggregatesRepository,
            TimeBoundaryService timeBoundaryService
    ) {
        return new DgraphSumAggregatorImpl(
                dgraphSumQueryBuilderService,
                dgraphEntityResolver,
                dgraphAggregatesRepository,
                timeBoundaryService
        );
    }

}
