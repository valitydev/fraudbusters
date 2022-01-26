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
import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

@Configuration
public class PaymentFraudoConfig {

    @Bean
    public CountPaymentAggregator<PaymentModel, PaymentCheckedField> countAggregator(
            PaymentRepository fraudResultRepository,
            RefundRepository refundRepository,
            ChargebackRepository chargebackRepository,
            DatabasePaymentFieldResolver databasePaymentFieldResolver) {
        return new CountAggregatorImpl(
                databasePaymentFieldResolver,
                fraudResultRepository,
                refundRepository,
                chargebackRepository
        );
    }

    @Bean
    public SumPaymentAggregator<PaymentModel, PaymentCheckedField> sumAggregator(
            PaymentRepository fraudResultRepository,
            RefundRepository refundRepository,
            ChargebackRepository chargebackRepository,
            DatabasePaymentFieldResolver databasePaymentFieldResolver) {
        return new SumAggregatorImpl(
                databasePaymentFieldResolver,
                fraudResultRepository,
                refundRepository,
                chargebackRepository
        );
    }


    @Bean
    public UniqueValueAggregator<PaymentModel, PaymentCheckedField> uniqueValueAggregator(
            PaymentRepository fraudResultRepository,
            DatabasePaymentFieldResolver databasePaymentFieldResolver) {
        return new UniqueValueAggregatorImpl(databasePaymentFieldResolver, fraudResultRepository);
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
            PaymentRepository fraudResultRepository,
            DatabasePaymentFieldResolver databasePaymentFieldResolver) {
        return new PaymentInListFinderImpl(wbListServiceSrv, databasePaymentFieldResolver, fraudResultRepository);
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
        VisitorBundle<PaymentModel, PaymentCheckedField> visitorBundle = getVisitorBundle(
                countAggregator,
                sumAggregator,
                uniqueValueAggregator,
                countryResolver,
                paymentInListFinder,
                paymentModelFieldResolver,
                paymentTypeResolver,
                customerTypeResolver);
        return new FraudVisitorFactoryImpl().createVisitor(visitorBundle);
    }

    @NotNull
    private VisitorBundle<PaymentModel, PaymentCheckedField> getVisitorBundle(
            CountPaymentAggregator<PaymentModel, PaymentCheckedField> countAggregator,
            SumPaymentAggregator<PaymentModel, PaymentCheckedField> sumAggregator,
            UniqueValueAggregator<PaymentModel, PaymentCheckedField> uniqueValueAggregator,
            CountryResolver<PaymentCheckedField> countryResolver,
            InListFinder<PaymentModel, PaymentCheckedField> paymentInListFinder,
            FieldResolver<PaymentModel, PaymentCheckedField> paymentModelFieldResolver,
            PaymentTypeResolver<PaymentModel> paymentTypeResolver,
            CustomerTypeResolver<PaymentModel> customerTypeResolver) {
        var aggregatorBundle = new AggregatorBundle<>(countAggregator, sumAggregator, uniqueValueAggregator);
        var resolverBundle = new ResolverBundle<>(
                countryResolver,
                paymentModelFieldResolver,
                new PaymentGroupResolver<>(paymentModelFieldResolver),
                new PaymentTimeWindowResolver(),
                paymentTypeResolver,
                customerTypeResolver);
        var finderBundle = new FinderBundle<>(paymentInListFinder);
        return new VisitorBundle<>(aggregatorBundle, resolverBundle, finderBundle);
    }


    @Bean
    public CountPaymentAggregator<PaymentModel, PaymentCheckedField> countResultAggregator(
            LocalResultStorageRepository localResultStorageRepository,
            PaymentRepository paymentRepositoryImpl,
            RefundRepository refundRepository,
            ChargebackRepository chargebackRepository,
            DatabasePaymentFieldResolver databasePaymentFieldResolver) {

        CountAggregatorImpl countAggregatorDecorator = new CountAggregatorImpl(
                databasePaymentFieldResolver,
                paymentRepositoryImpl,
                refundRepository,
                chargebackRepository
        );
        return new LocalCountAggregatorDecorator(
                countAggregatorDecorator,
                databasePaymentFieldResolver,
                localResultStorageRepository
        );
    }

    @Bean
    public SumPaymentAggregator<PaymentModel, PaymentCheckedField> sumResultAggregator(
            LocalResultStorageRepository localResultStorageRepository,
            PaymentRepository paymentRepositoryImpl,
            RefundRepository refundRepository,
            ChargebackRepository chargebackRepository,
            DatabasePaymentFieldResolver databasePaymentFieldResolver) {

        SumAggregatorImpl sumAggregator = new SumAggregatorImpl(
                databasePaymentFieldResolver,
                paymentRepositoryImpl,
                refundRepository,
                chargebackRepository
        );
        return new LocalSumAggregatorDecorator(
                sumAggregator,
                databasePaymentFieldResolver,
                localResultStorageRepository
        );
    }

    @Bean
    public UniqueValueAggregator<PaymentModel, PaymentCheckedField> uniqueValueResultAggregator(
            LocalResultStorageRepository localResultStorageRepository,
            PaymentRepository fraudResultRepository,
            DatabasePaymentFieldResolver databasePaymentFieldResolver) {
        UniqueValueAggregatorImpl uniqueValueAggregator =
                new UniqueValueAggregatorImpl(databasePaymentFieldResolver, fraudResultRepository);

        return new LocalUniqueValueAggregatorDecorator(
                uniqueValueAggregator,
                databasePaymentFieldResolver,
                localResultStorageRepository
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
        VisitorBundle<PaymentModel, PaymentCheckedField> visitorBundle = getVisitorBundle(
                countResultAggregator,
                sumResultAggregator,
                uniqueValueResultAggregator,
                countryResolver,
                paymentInListFinder,
                paymentModelFieldResolver,
                paymentTypeResolver,
                customerTypeResolver);
        return new FullVisitorFactoryImpl().createVisitor(visitorBundle);
    }

    @Bean
    @Lazy
    public UniqueValueAggregator<PaymentModel, PaymentCheckedField> dgraphUniqueAggregator(
            DgraphAggregationQueryBuilderService dgraphUniqueQueryBuilderService,
            DgraphEntityResolver dgraphEntityResolver,
            DgraphAggregatesRepository dgraphAggregatesRepository,
            DatabasePaymentFieldResolver databasePaymentFieldResolver) {
        return new DgraphUniqueAggregatorImpl(
                dgraphUniqueQueryBuilderService,
                dgraphEntityResolver,
                dgraphAggregatesRepository,
                databasePaymentFieldResolver
        );
    }

    @Bean
    @Lazy
    public CountPaymentAggregator<PaymentModel, PaymentCheckedField> dgraphCountAggregator(
            DgraphAggregationQueryBuilderService dgraphCountQueryBuilderService,
            DgraphEntityResolver dgraphEntityResolver,
            DgraphAggregatesRepository dgraphAggregatesRepository
    ) {
        return new DgraphCountAggregatorImpl(
                dgraphCountQueryBuilderService,
                dgraphEntityResolver,
                dgraphAggregatesRepository
        );
    }

    @Bean
    @Lazy
    public SumPaymentAggregator<PaymentModel, PaymentCheckedField> dgraphSumAggregator(
            DgraphAggregationQueryBuilderService dgraphSumQueryBuilderService,
            DgraphEntityResolver dgraphEntityResolver,
            DgraphAggregatesRepository dgraphAggregatesRepository
    ) {
        return new DgraphSumAggregatorImpl(
                dgraphSumQueryBuilderService,
                dgraphEntityResolver,
                dgraphAggregatesRepository
        );
    }

}
