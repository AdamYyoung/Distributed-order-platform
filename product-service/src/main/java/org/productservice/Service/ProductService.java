package org.productservice.Service;

import org.productservice.DAO.Product;

import java.util.List;

public interface ProductService {
    public Product getProduct(Long id);
    public Product getProductWithCheck(Long id);
    public List<Product> listOnSales();
    public Product addProduct(Product product);
    public Product updateProduct(Long id, Product product);
    public void deleteProduct(Long id);
}
