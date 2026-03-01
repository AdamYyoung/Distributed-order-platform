package org.productservice.Service.Impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.commonlib.Exception.BizException;
import org.commonlib.Exception.ErrorCode;
import org.productservice.DAO.Product;
import org.productservice.Repository.ProductRepo;
import org.productservice.Service.ProductService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductServiceImpl implements ProductService {

    private final ProductRepo productRepo;
    private final ApplicationContext applicationContext;

    // TODO: cache
    // TODO: log
    // TODO: transaction

    @Override
    public Product getProductWithCheck(Long id) {
        ProductService proxy = applicationContext.getBean(ProductService.class); // avoid self-invocation
        Product p = proxy.getProduct(id);
        if (p == null || p.getId() == null) {
            throw new BizException(ErrorCode.PRODUCT_NOT_FOUND);
        }
        return p;
    }

    @Override
    @Cacheable(value = "product", key = "#id")
    public Product getProduct(Long id) {
        log.info("--- [Cache Miss] Fetching product {} from DB ---", id);
        return productRepo.findById(id)
                .filter(p -> p.getStatus() != 0)
                .orElse(null); // avoid cache penetration
    }

    @Override
    @Transactional
    public List<Product> listOnSales() {
        return productRepo.findByStatus(1);
    }

    @Override
    @Transactional
    public Product addProduct(Product product) {
        return productRepo.save(product);
    }

    @Override
    @Transactional
    @CacheEvict(value = "product", key = "#id")
    public Product updateProduct(Long id, Product product) {
        return productRepo.findById(id).map(exisiting -> {
            if(product.getName() != null) exisiting.setName(product.getName());
            if(product.getPrice() != null) exisiting.setPrice(product.getPrice());
            if(product.getDescription() != null) exisiting.setDescription(product.getDescription());
            if(product.getStatus() != null) exisiting.setStatus(product.getStatus());
            return productRepo.save(exisiting);
        }).orElseThrow(()->new BizException(ErrorCode.PRODUCT_NOT_FOUND));
    }

    @Override
    @Transactional
    @CacheEvict(value = "product", key = "#id")
    public void deleteProduct(Long id) {
        productRepo.findById(id).map(exisiting -> {
            exisiting.setStatus(0);
            return productRepo.save(exisiting);
        }).orElseThrow(()->new BizException(ErrorCode.PRODUCT_NOT_FOUND));
    }
}
