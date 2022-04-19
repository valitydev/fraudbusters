package dev.vality.fraudbusters.listener;

import dev.vality.kafka.common.loader.PreloadListener;
import dev.vality.kafka.common.loader.PreloadListenerImpl;
import dev.vality.damsel.fraudbusters.Command;
import dev.vality.fraudbusters.config.properties.KafkaTopics;
import dev.vality.fraudbusters.exception.StartException;
import dev.vality.fraudbusters.listener.payment.GroupListener;
import dev.vality.fraudbusters.listener.payment.GroupReferenceListener;
import dev.vality.fraudbusters.listener.payment.TemplateListener;
import dev.vality.fraudbusters.listener.payment.TemplateReferenceListener;
import dev.vality.fraudbusters.pool.Pool;
import dev.vality.fraudbusters.service.CardPoolManagementService;
import dev.vality.fraudbusters.service.PoolMonitoringService;
import dev.vality.fraudbusters.stream.StreamManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.antlr.v4.runtime.ParserRuleContext;
import org.apache.kafka.clients.consumer.Consumer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class StartupListener implements ApplicationListener<ContextRefreshedEvent> {

    private static final int COUNT_PRELOAD_TASKS = 4;

    private final StreamManager streamManager;

    private final ConsumerFactory<String, Command> templateListenerFactory;
    private final ConsumerFactory<String, Command> groupListenerFactory;
    private final ConsumerFactory<String, Command> referenceListenerFactory;
    private final ConsumerFactory<String, Command> groupReferenceListenerFactory;

    private final TemplateListener templateListener;
    private final GroupListener groupListener;
    private final GroupReferenceListener groupReferenceListener;
    private final TemplateReferenceListener templateReferenceListener;

    private final ConsumerFactory<String, Command> timeTemplateListenerFactory;
    private final ConsumerFactory<String, Command> timeGroupListenerFactory;
    private final ConsumerFactory<String, Command> timeReferenceListenerFactory;
    private final ConsumerFactory<String, Command> timeGroupReferenceListenerFactory;

    private final TemplateListener timeTemplateListener;
    private final GroupListener timeGroupListener;
    private final GroupReferenceListener timeGroupReferenceListener;
    private final TemplateReferenceListener timeTemplateReferenceListener;

    private final Pool<ParserRuleContext> templatePoolImpl;

    private final PreloadListener<String, Command> preloadListener = new PreloadListenerImpl<>();
    private final KafkaTopics kafkaTopics;

    private final PoolMonitoringService poolMonitoringService;
    private final CardPoolManagementService cardPoolManagementService;

    @Value("${preload.timeout:20}")
    private long preloadTimeout;

    @Value("${kafka.historical.listener.enable}")
    private boolean historicalListenerEnabled;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        try {
            final long startPreloadTime = System.currentTimeMillis();

            poolMonitoringService.addPoolsToMonitoring();

            List<Runnable> tasks = new ArrayList<>();
            CountDownLatch latch;
            if (historicalListenerEnabled) {
                latch = new CountDownLatch(COUNT_PRELOAD_TASKS + 4);
                initRewriteStream();
                tasks.addAll(List.of(
                        () -> waitPreLoad(
                                latch,
                                timeTemplateListenerFactory,
                                kafkaTopics.getFullTemplate(),
                                timeTemplateListener
                        ),
                        () -> waitPreLoad(
                                latch,
                                timeReferenceListenerFactory,
                                kafkaTopics.getFullReference(),
                                timeTemplateReferenceListener
                        ),
                        () -> waitPreLoad(
                                latch,
                                timeGroupListenerFactory,
                                kafkaTopics.getFullGroupList(),
                                timeGroupListener
                        ),
                        () -> waitPreLoad(
                                latch,
                                timeGroupReferenceListenerFactory,
                                kafkaTopics.getFullGroupReference(),
                                timeGroupReferenceListener
                        )
                ));
            } else {
                latch = new CountDownLatch(COUNT_PRELOAD_TASKS);
            }

            ExecutorService executorService = Executors.newFixedThreadPool(COUNT_PRELOAD_TASKS);

            tasks.add(cardPoolManagementService::updateTrustedTokens);

            tasks.addAll(List.of(
                    () -> waitPreLoad(latch, templateListenerFactory, kafkaTopics.getTemplate(), templateListener),
                    () -> waitPreLoad(
                            latch,
                            referenceListenerFactory,
                            kafkaTopics.getReference(),
                            templateReferenceListener
                    ),
                    () -> waitPreLoad(latch, groupListenerFactory, kafkaTopics.getGroupList(), groupListener),
                    () -> waitPreLoad(
                            latch,
                            groupReferenceListenerFactory,
                            kafkaTopics.getGroupReference(),
                            groupReferenceListener
                    )
                    )
            );

            tasks.forEach(executorService::submit);
            long timeout = preloadTimeout * COUNT_PRELOAD_TASKS;
            boolean await = latch.await(timeout, TimeUnit.SECONDS);

            if (!await) {
                throw new StartException("Cant load all rules by timeout: " + timeout);
            }

            log.info("StartupListener start stream preloadTime: {} ms", System.currentTimeMillis() - startPreloadTime);
            log.info(
                    "StartupListener load pool payment template size: {} templates: {}",
                    templatePoolImpl.size(),
                    templatePoolImpl
            );
        } catch (InterruptedException e) {
            log.error("StartupListener onApplicationEvent e: ", e);
            Thread.currentThread().interrupt();
        }
    }

    private void initRewriteStream() {
        streamManager.createStream(kafkaTopics.getFullTemplate(), kafkaTopics.getTemplate(), "template-stream");
        streamManager.createStream(kafkaTopics.getFullReference(), kafkaTopics.getReference(), "reference-stream");
        streamManager.createStream(kafkaTopics.getFullGroupList(), kafkaTopics.getGroupList(), "group-stream");
        streamManager.createStream(
                kafkaTopics.getFullGroupReference(),
                kafkaTopics.getGroupReference(),
                "group-ref-stream"
        );
    }

    private void waitPreLoad(
            CountDownLatch latch,
            ConsumerFactory<String, Command> groupListenerFactory,
            String topic,
            CommandListener listener) {
        try (Consumer<String, Command> consumer = groupListenerFactory.createConsumer()) {
            preloadListener.preloadToLastOffsetInPartition(consumer, topic, 0, listener::listen);
        }
        latch.countDown();
    }

}
