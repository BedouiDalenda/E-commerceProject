package dev.dberenguer.dapr.product.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.dberenguer.dapr.product.dto.ProductDto;
import dev.dberenguer.dapr.product.model.Product;
import io.dapr.client.DaprClient;
import io.dapr.client.domain.State;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Service;
import org.springframework.context.event.EventListener;
import org.springframework.boot.context.event.ApplicationReadyEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


@Slf4j
@RequiredArgsConstructor
@Service
public class ProductService {

    private static final String CACHE_NAME_PRODUCT_ALL = "product:all";

    private final DaprClient daprClient;
    private final ObjectMapper objectMapper;
    private final ConversionService conversionService;

    @Value("${dberenguer.dapr.state-store.name}")
    private String daprStateStoreName;

    public List<ProductDto> findAll() {
        final State<List> stateProducts = this.daprClient
                .getState(this.daprStateStoreName, CACHE_NAME_PRODUCT_ALL, List.class)
                .block();

        List<Product> products = stateProducts.getValue().stream().map(p -> objectMapper.convertValue(p, Product.class)).toList();

        log.info("Getting state products: {}", products);

        // Conversion Entity -> DTO
        return products.stream()
                .map(p -> conversionService.convert(p, ProductDto.class))
                .toList();
    }

    public ProductDto create(ProductDto productDto) {
    // Récupérer la liste des Product existants
    final State<List> stateProducts = daprClient
            .getState(daprStateStoreName, CACHE_NAME_PRODUCT_ALL, List.class)
            .block();

    List<Product> products = new ArrayList<>();
    if (stateProducts != null && stateProducts.getValue() != null) {
        for (Object obj : stateProducts.getValue()) {
            products.add(objectMapper.convertValue(obj, Product.class));
        }
    }

    // Créer la nouvelle Entity
    Product newProduct = Product.builder()
            .id(UUID.randomUUID())
            .code(productDto.getCode())
            .name(productDto.getName())
            .price(productDto.getPrice())
            .build();

    // Ajouter et sauvegarder
    products.add(newProduct);
    daprClient.saveState(daprStateStoreName, CACHE_NAME_PRODUCT_ALL, products).block();

    log.info("Product created: {}", newProduct);

    // Retourner DTO pour l'API
    return conversionService.convert(newProduct, ProductDto.class);
}

@EventListener(ApplicationReadyEvent.class)
public void initAfterStartup() {
    try {
        State<List> state = daprClient
                .getState(daprStateStoreName, CACHE_NAME_PRODUCT_ALL, List.class)
                .block();

        if (state == null || state.getValue() == null || state.getValue().isEmpty()) {
            List<Product> products = List.of(
                    Product.builder().id(UUID.randomUUID()).code("1").name("computer").price(999.99).build(),
                    Product.builder().id(UUID.randomUUID()).code("2").name("mobile").price(499.90).build(),
                    Product.builder().id(UUID.randomUUID()).code("3").name("keyboard").price(49.95).build()
            );

            daprClient.saveState(daprStateStoreName, CACHE_NAME_PRODUCT_ALL, products).block();
            log.info("Initial products saved");
        }
    } catch (Exception e) {
        log.warn("Dapr not ready yet, skipping init");
    }
}

}