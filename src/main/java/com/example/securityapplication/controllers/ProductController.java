package com.example.securityapplication.controllers;



import com.example.securityapplication.models.Category;
import com.example.securityapplication.models.Product;
import com.example.securityapplication.models.Provider;
import com.example.securityapplication.repositories.CategoryRepository;
import com.example.securityapplication.repositories.ProductRepository;
import com.example.securityapplication.response.AddProductResponse;
import com.example.securityapplication.security.PersonReactDetails;
import com.example.securityapplication.services.CategoryService;
import com.example.securityapplication.services.ProductService;
import com.example.securityapplication.services.ProviderService;
import com.example.securityapplication.util.ProductValidator;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import javax.validation.Valid;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@RestController
@CrossOrigin(methods = {RequestMethod.POST, RequestMethod.GET, RequestMethod.OPTIONS, RequestMethod.HEAD, RequestMethod.DELETE})
public class ProductController {

    private final ProductService productService;
    private final ProductRepository productRepository;
    private final ProductValidator productValidator;
    private final CategoryService categoryService;
    private final CategoryRepository categoryRepository;
    private final ProviderService providerService;

    @Value("${upload.path}")
    private String uploadPath;

    public ProductController(ProductService productService, ProductRepository productRepository, ProductValidator productValidator, CategoryService categoryService, CategoryRepository categoryRepository, ProviderService providerService) {
        this.productService = productService;
        this.productRepository = productRepository;
        this.productValidator = productValidator;
        this.categoryService = categoryService;
        this.categoryRepository = categoryRepository;
        this.providerService = providerService;
    }

    @GetMapping("/main/api/products")
    public List<Product> getProducts() {
        return productRepository.findAll();
    }

    @GetMapping("/main/api/products/{id}")
    public List<Product> getProductsByProvider(@PathVariable int id){
        return productRepository.findAllByProvider_Id(id);
    }

    @DeleteMapping("/main/api/product/delete/{id}")
    public ResponseEntity<?> deleteProductById(@PathVariable int id){
        Optional<Product> product = productRepository.findById(id);
        AddProductResponse response = new AddProductResponse();
        if (product.isEmpty()){
            response.setMessage("?????????? ???? ?????????????????? ????????????????");
            return new ResponseEntity<>(response, HttpStatus.NOT_ACCEPTABLE);
        } else {
            productService.deleteById(id);
            response.setMessage("?????????? ????????????");
            return ResponseEntity.ok(response);
        }

    }

   // @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @PostMapping("main/api/product/avatar")
    public ResponseEntity<?> postProductAvatarAdmin(@RequestParam("file") MultipartFile file, @RequestParam("name") String name) throws IOException {
        System.out.println("???????????????????????? ????????????: " + name);
        AddProductResponse response = new AddProductResponse();
        Optional<Product> product = productRepository.findByName(name);
        if (product.isEmpty()){
            response.setMessage("?????????? ???? ?????????????????? ????????????????");
            return new ResponseEntity<>(response, HttpStatus.NOT_ACCEPTABLE);
        } else {
            if (file != null) {
                File uploadDir = new File(uploadPath + "/product");
                if (!uploadDir.exists()) {
                    uploadDir.mkdir();
                }
                String uuidFile = UUID.randomUUID().toString();
                String resultFileName = uuidFile + "_" + file.getOriginalFilename();
                file.transferTo(new File(uploadPath + "/product/" + resultFileName));
                int id = product.get().getId();
                System.out.println("ID ?? ???????????? : " + id);
                product.get().setFileName(resultFileName);
                productService.save(product.get());

                response.setMessage("?????????????? ???????????????? ?????????????????????? ????????????");
                System.out.println("C?????????? ?????????????????? ?? " + product.get().getName() + " ????????: " + product.get().getFileName());
                return ResponseEntity.ok(response);
            } else {
                response.setMessage("?????????????????????? ???????????? ???? ?????????????? ??????????????");
                return new ResponseEntity<>(response, HttpStatus.NOT_ACCEPTABLE);
            }
        }
    }

    @PostMapping("/main/api/product/update")
    public ResponseEntity<?> postProductUpdateAdmin(@RequestBody @Valid Product product, BindingResult bindingResult) {

        AddProductResponse response = new AddProductResponse();
        if (bindingResult.hasErrors()) {
            List<FieldError> errors = bindingResult.getFieldErrors();
            for (FieldError error : errors) {
                System.out.println(error.getField() + " - " + error.getDefaultMessage());
                switch (error.getField()) {
                    case "name":
                        response.setName(error.getDefaultMessage());
                        break;
                    case "description":
                        response.setDescription(error.getDefaultMessage());
                        break;
                    case "price":
                        response.setPrice(error.getDefaultMessage());
                        break;
                    case "provider":
                        response.setProvider(error.getDefaultMessage());
                    default:
                        response.setMessage("?????????????????????? ???????????? ???? ?????????????? ??????????????");
                }
            }


            return new ResponseEntity<>(response, HttpStatus.NOT_ACCEPTABLE);
        }
        Optional<Product> productInBase = productRepository.findById(product.getId());
        if (productInBase.isEmpty()) {
            response.setMessage("?????????????? ???? ?????????????????? ????????????????");
            return new ResponseEntity<>(response, HttpStatus.NOT_ACCEPTABLE);
        } else {

            Category category = categoryService.findCategoryById(product.getCat_id());
            Optional<Provider> provider = providerService.findById(product.getProv_id());
            if(provider.isEmpty()){
                response.setMessage("???????????? - ???? ???????????? ?????????????????? ????????????");
                return new ResponseEntity<>(response, HttpStatus.NOT_ACCEPTABLE);
            }
            productInBase.get().setProvider(provider.get());
            productInBase.get().setProviderName(provider.get().getLogin());

            if(category != null) {
                productInBase.get().setCategory(category);
                productInBase.get().setNameCategory(category.getName());
            }


            productInBase.get().setName(product.getName());
            productInBase.get().setDescription(product.getDescription());
            productInBase.get().setPrice(product.getPrice());


            productService.save(productInBase.get());

            response.setMessage("?????????????? ???????????????? ??????????????");
            return ResponseEntity.ok(response);
        }

    }

    @PostMapping("/main/api/product/add")
    public ResponseEntity<?> postProductNewAdmin(@RequestBody @Valid Product product, BindingResult bindingResult) {
        System.out.println("ID ???????????? : " + product.getId());
        System.out.println("???????????????? ???????????? : " + product.getName());
        System.out.println("???????????????? ???????????? : " + product.getDescription());

        AddProductResponse response = new AddProductResponse();
        productValidator.validate(product, bindingResult);
        if (bindingResult.hasErrors()) {
            System.out.println("?????????? ????????????: " + bindingResult.getAllErrors());
            System.out.println(bindingResult.getFieldError());
            System.out.println(bindingResult.getFieldValue("name"));

            List<FieldError> errors = bindingResult.getFieldErrors();
            for (FieldError error : errors) {
                System.out.println(error.getField() + " - " + error.getDefaultMessage());
                switch (error.getField()) {
                    case "name":
                        response.setName(error.getDefaultMessage());
                        break;
                    case "description":
                        response.setDescription(error.getDefaultMessage());
                        break;
                    case "price":
                        response.setPrice(error.getDefaultMessage());
                        break;
                    case "provider":
                        response.setProvider(error.getDefaultMessage());
                        break;
                    case "category_id":
                        response.setNumber(error.getDefaultMessage());
                    default:
                        response.setMessage("?????????????????????? ???????????? ???? ?????????????? ??????????????");
                }
            }


            return new ResponseEntity<>(response, HttpStatus.NOT_ACCEPTABLE);
        }

        Category category = categoryService.findCategoryById(product.getCat_id());
        Optional<Provider> provider = providerService.findById(product.getProv_id());
        if(provider.isEmpty()){
            response.setMessage("???????????? - ???? ???????????? ?????????????????? ????????????");
            return new ResponseEntity<>(response, HttpStatus.NOT_ACCEPTABLE);
        }
        product.setProvider(provider.get());
        product.setProviderName(provider.get().getLogin());

        if(category != null) {
            product.setCategory(category);
            product.setNameCategory(category.getName());
        }
        productService.save(product);

        response.setMessage("?????????????? ???????????????? ??????????");
        return ResponseEntity.ok(response);
    }

    @GetMapping(value = "/main/api/product/image/get/{filename:.+}", produces = MediaType.IMAGE_PNG_VALUE)
    public @ResponseBody byte[] getImageProductJpg(@PathVariable String filename) throws IOException {

        boolean isFileExist = false;
        Path pathFile = Paths.get(uploadPath+ "/product/iphone.png");
        if(Files.exists(pathFile)) isFileExist = true;

        if(filename.isEmpty() && !isFileExist) {
            return null;
        }
        if(filename.isEmpty() && isFileExist){
            InputStream infile = new FileInputStream(pathFile.toString());
            return IOUtils.toByteArray(infile);
        }
        if(!filename.isEmpty()) {
            Path path = Paths.get(uploadPath + "/product/" + filename);
            if(Files.exists(path)){
                InputStream infile = new FileInputStream(path.toString());
                return IOUtils.toByteArray(infile);
            } else {
                if(isFileExist) {
                    InputStream infile = new FileInputStream(pathFile.toString());
                    return IOUtils.toByteArray(infile);
                } else {
                    return null;
                }
            }
        }
        return null;
    }

    @GetMapping("/main/api/product/category")
    public List<Category> getCategories() {
        return categoryRepository.findAll();
    }

    @GetMapping("/main/api/product/category/provider")
    public List<Category> getCategoriesProvider() {
        return categoryRepository.findAll();
    }

    @GetMapping("/main/api/product/category/all")
    public List<Category> getCategoriesForAll() {
        return categoryRepository.findAll();
    }

    // ???????? ?????????? ?????? ?????????????? ???????? ?????? ?????????????? ?????????????? (???????????????? : ??????????????) ?? ???????????? ???????????????????? ?? ????????????????????
    // ?????? ???????????????????? ???????????????????? ???? ???????????????????????? ????????????, ???? ?????????????????? ?????? ???? ?? ???? ????????????, ???? ?????????????????? ????????????
    // ?????????? ?????????????? ?? ????????????????????????????, ???????????????? ?????? ???????????????????????? ????????????????????
    @PostMapping("/search")
    public String getAll(Model model, @RequestParam(value = "search", required = false) String search,
                         @RequestParam(value = "category", required = false)String category,
                         @RequestParam(value = "Ot", required = false) String Ot,
                         @RequestParam(value = "Do", required = false) String Do,
                         @RequestParam(value = "price", defaultValue = "asc") String sort)
    {
        System.out.println("???????????? ????????????: " + category + " " + sort + " " + search);
        if(!search.isEmpty()) {
            System.out.println("?????? ???????????? ???????? ???????? ???????? search");
            if(!Ot.isEmpty() && !Do.isEmpty()){
                if(category != null && !category.equals("")){
                    List<Category> categoryList = categoryRepository.findAll();
                    for(Category category1: categoryList){
                        if(category.equals(category1.getName())){
                            if(sort.equals("desc")){
                                model.addAttribute("search_product", productRepository.findByTitleAndCategoryOrderByPriceDesc(search.toLowerCase(), Float.parseFloat(Ot), Float.parseFloat(Do), category1.getId()));
                            } else {
                                model.addAttribute("search_product", productRepository.findByTitleAndCategoryOrderByPriceAsc(search.toLowerCase(), Float.parseFloat(Ot), Float.parseFloat(Do), category1.getId()));
                            }
                        }
                    }
                } else {
                    model.addAttribute("search_product", productRepository.findByTitleAndPriceGreaterThanEqualAndPriceLessThanEqual(search, Float.parseFloat(Ot), Float.parseFloat(Do)));
                }
            } else if(Ot.isEmpty() && !(Do.isEmpty())) {
                if(category != null && !category.equals("")){
                    List<Category> categoryList = categoryRepository.findAll();
                    for(Category category1: categoryList){
                        if(category.equals(category1.getName())){
                            if(sort.equals("desc")){
                                model.addAttribute("search_product", productRepository.findByTitleAndCategoryOrderByPriceDesc(search.toLowerCase(), 1, Float.parseFloat(Do), category1.getId()));
                            } else {
                                model.addAttribute("search_product", productRepository.findByTitleAndCategoryOrderByPriceAsc(search.toLowerCase(), 1, Float.parseFloat(Do), category1.getId()));
                            }
                        }
                    }
                } else {
                    if(sort.equals("desc")){
                        model.addAttribute("search_product", productRepository.findByTitleOrderByPriceDest(search.toLowerCase(), 1, Float.parseFloat(Do)));
                    } else {
                        model.addAttribute("search_product", productRepository.findByTitleOrderByPriceAsc(search.toLowerCase(), 1, Float.parseFloat(Do)));
                    }
                }
            } else if(!(Ot.isEmpty()) && Do.isEmpty()) {
                if (category != null && !category.equals("")){
                    List<Category> categoryList = categoryRepository.findAll();
                    for(Category category1: categoryList){
                        if(category.equals(category1.getName())){
                            if(sort.equals("desc")){
                                model.addAttribute("search_product", productRepository.findByTitleAndCategoryOrderByPriceDesc(search.toLowerCase(), Float.parseFloat(Ot), Float.MAX_VALUE, category1.getId()));
                            } else {
                                model.addAttribute("search_product", productRepository.findByTitleAndCategoryOrderByPriceAsc(search.toLowerCase(), Float.parseFloat(Ot), Float.MAX_VALUE, category1.getId()));
                            }
                        }
                    }
                } else {
                    if(sort.equals("desc")){
                        model.addAttribute("search_product", productRepository.findByTitleOrderByPriceDest(search.toLowerCase(), Float.parseFloat(Ot), Float.MAX_VALUE));
                    } else {
                        model.addAttribute("search_product", productRepository.findByTitleOrderByPriceAsc(search.toLowerCase(), Float.parseFloat(Ot), Float.MAX_VALUE));
                    }
                }
            } else {
                System.out.println("?????? ???????????? ???????? ???????? ?????? ???? ?? ????");
                if(category != null && !category.equals("")) {
                    List<Category> categoryList = categoryRepository.findAll();
                    for(Category category1: categoryList){
                        if(category.equals(category1.getName())){
                            if(sort.equals("desc")){
                                model.addAttribute("search_product", productRepository.findByTitleAndCategoryOrderByPriceDesc(search.toLowerCase(), 1, Float.MAX_VALUE, category1.getId()));
                            } else {
                                model.addAttribute("search_product", productRepository.findByTitleAndCategoryOrderByPriceAsc(search.toLowerCase(), 1, Float.MAX_VALUE, category1.getId()));
                            }
                        }
                    }
                } else {
                    if(sort.equals("desc")){
                        model.addAttribute("search_product", productRepository.findByTitleOrderByPriceDest(search.toLowerCase(), 1, Float.MAX_VALUE));
                    } else {
                        model.addAttribute("search_product", productRepository.findByTitleOrderByPriceAsc(search.toLowerCase(), 1, Float.MAX_VALUE));
                    }
                }
            }
        } else {
            System.out.println("???????????????????????? ????????????, ????????????????");
            if(!Ot.isEmpty() && !Do.isEmpty()){
                if (category != null && !category.equals("")){
                    List<Category> categoryList = categoryRepository.findAll();
                    for(Category category1: categoryList){
                        if(category.equals(category1.getName())){
                            if(sort.equals("desc")){
                                model.addAttribute("search_product", productRepository.findAllByCategoryOrderByPriceDesc( Float.parseFloat(Ot), Float.parseFloat(Do), category1.getId()));
                            } else {
                                model.addAttribute("search_product", productRepository.findAllByCategoryOrderByPriceAsc(Float.parseFloat(Ot), Float.parseFloat(Do), category1.getId()));
                            }
                        }
                    }
                } else {
                    if(sort.equals("desc")){
                        model.addAttribute("search_product", productRepository.findAllByPriceOrderByPriceDesc( Float.parseFloat(Ot), Float.parseFloat(Do)));
                    } else {
                        model.addAttribute("search_product", productRepository.findAllByPriceOrderByPriceAsc( Float.parseFloat(Ot), Float.parseFloat(Do)));
                    }
                }
            } else if(Ot.isEmpty() && !Do.isEmpty()){
                if (category != null && !category.equals("")){
                    List<Category> categoryList = categoryRepository.findAll();
                    for(Category category1: categoryList){
                        if(category.equals(category1.getName())){
                            if(sort.equals("desc")){
                                model.addAttribute("search_product", productRepository.findAllByCategoryOrderByPriceDesc( 1, Float.parseFloat(Do), category1.getId()));
                            } else {
                                model.addAttribute("search_product", productRepository.findAllByCategoryOrderByPriceAsc(1, Float.parseFloat(Do), category1.getId()));
                            }
                        }
                    }
                } else {
                    if(sort.equals("desc")){
                        model.addAttribute("search_product", productRepository.findAllByPriceOrderByPriceDesc( 1, Float.parseFloat(Do)));
                    } else {
                        model.addAttribute("search_product", productRepository.findAllByPriceOrderByPriceAsc( 1, Float.parseFloat(Do)));
                    }
                }
            } else if(!Ot.isEmpty() && Do.isEmpty()) {
                if (category != null && !category.equals("")){
                    List<Category> categoryList = categoryRepository.findAll();
                    for(Category category1: categoryList){
                        if(category.equals(category1.getName())){
                            if(sort.equals("desc")){
                                model.addAttribute("search_product", productRepository.findAllByCategoryOrderByPriceDesc( Float.parseFloat(Ot), Float.MAX_VALUE, category1.getId()));
                            } else {
                                model.addAttribute("search_product", productRepository.findAllByCategoryOrderByPriceAsc(Float.parseFloat(Ot), Float.MAX_VALUE, category1.getId()));
                            }
                        }
                    }
                } else {
                    if(sort.equals("desc")){
                        model.addAttribute("search_product", productRepository.findAllByPriceOrderByPriceDesc( Float.parseFloat(Ot), Float.MAX_VALUE));
                    } else {
                        model.addAttribute("search_product", productRepository.findAllByPriceOrderByPriceAsc( Float.parseFloat(Ot), Float.MAX_VALUE));
                    }
                }
            } else {
                System.out.println("???? ?? ???? ???????? ???????????? ????????????????");
                if (category != null && !category.equals("")){
                    System.out.println("?????????????????? ????????????-???? ???? ????????????");
                    List<Category> categoryList = categoryRepository.findAll();
                    for(Category category1: categoryList){
                        if(category.equals(category1.getName())){
                            if(sort.equals("desc")){
                                System.out.println("?????????? ??????????????????");
                                model.addAttribute("search_product", productRepository.findAllByCategoryOrderByPriceDesc( 1, Float.MAX_VALUE, category1.getId()));
                                List<Product> productList = productRepository.findAllByCategoryOrderByPriceDesc(50000, 1, category1.getId());
                                for (Product product: productList){
                                    // System.out.println("?????????? ????????????: " + product.getTitle());
                                }
                            } else {
                                model.addAttribute("search_product", productRepository.findAllByCategoryOrderByPriceAsc(1, Float.MAX_VALUE, category1.getId()));
                            }
                        }
                    }
                } else {
                    if(sort.equals("desc")){
                        model.addAttribute("search_product", productRepository.findAllByPriceOrderByPriceDesc( 1, Float.MAX_VALUE));
                        System.out.println("???????????????????? ???? ????????????????");
                    } else {
                        model.addAttribute("search_product", productRepository.findAllByPriceOrderByPriceAsc( 1, Float.MAX_VALUE));
                        System.out.println("???????????????????? ???? ??????????????????????");
                    }
                }
            }
        }
        List<Category> categoryList = categoryRepository.findAll();
        List<String> categoryName = new ArrayList<>();
        for(Category category1: categoryList){
            categoryName.add(category1.getName());
        }

        model.addAttribute("value_search", search);
        model.addAttribute("value_price_ot", Ot);
        model.addAttribute("value_price_do", Do);
        //model.addAttribute("products", productService.getAllProduct());
        model.addAttribute("category", categoryName);
        try{
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            PersonReactDetails personDetails = (PersonReactDetails) authentication.getPrincipal();
            String role = personDetails.getPerson().getRole();
            if(role.equals("ROLE_USER"))
            {
                return "/product/productUser";
            }
        } catch (Exception e){
            return "/product/product";
        }


        return "/product/product";
    }


}
