package org.productservice.Controller;

import lombok.RequiredArgsConstructor;
import org.commonlib.Response.ApiResponse;
import org.productservice.DAO.Product;
import org.productservice.Service.ProductService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/product")
@RequiredArgsConstructor
public class ProductController {
    private final ProductService productService;
    @GetMapping("/{id}")
    public ApiResponse<Product> getProduct(@PathVariable("id") Long id) {
        return ApiResponse.success(productService.getProductWithCheck(id));
    }

    @GetMapping
    public ApiResponse<List<Product>> getProductsOnSales() {
        return ApiResponse.success(productService.listOnSales());
    }

    @PostMapping
    public ApiResponse<Product> addProduct(@RequestBody Product product) {
        return ApiResponse.success(productService.addProduct(product));
    }

    @PutMapping("/{id}")
    public ApiResponse<Product> updateProduct(@PathVariable Long id, @RequestBody Product product) {
        return ApiResponse.success(productService.updateProduct(id, product));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<String> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ApiResponse.success("Delete success");
    }


}
