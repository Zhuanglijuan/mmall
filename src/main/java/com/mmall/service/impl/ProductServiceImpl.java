package com.mmall.service.impl;

import com.mmall.common.ResponseCode;
import com.mmall.common.ServerResponse;
import com.mmall.dao.CategoryMapper;
import com.mmall.dao.ProductMapper;
import com.mmall.pojo.Category;
import com.mmall.pojo.Product;
import com.mmall.service.IProductService;
import com.mmall.util.DateTimeUtil;
import com.mmall.util.PropertiesUtil;
import com.mmall.vo.ProductDetailVo;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Created by Zhuang on 2018/5/16.
 */
@Service("iProductService")
public class ProductServiceImpl implements IProductService {
    @Autowired
    private ProductMapper productMapper;
    @Autowired
    private CategoryMapper categoryMapper;
    /**
     * 新增或更新产品
     * @param product   :产品
     * @return  :响应体
     */
    public ServerResponse saveOrUpdateProduct(Product product){
        if(product != null){
            //如果子图不为空
            if(StringUtils.isBlank(product.getSubImages())){
                //取第一个子图为主图
                String[] subImageArray = product.getSubImages().split(",");
                if(subImageArray.length > 0){
                    product.setMainImage(subImageArray[0]);
                }
            }

            if(product.getId() != null){//产品有id为更新操作
                int rowCount = productMapper.updateByPrimaryKey(product);
                //操作成功
                if(rowCount > 0){
                    return ServerResponse.createBySuccess("更新产品成功");
                }else{//操作失败
                    return ServerResponse.createByErrorMessage("更新产品失败");
                }
            }else{//无id为增加操作
                int rowCount = productMapper.insert(product);
                if(rowCount > 0){//操作成功
                    return ServerResponse.createBySuccess("新增产品成功");
                }else{//操作失败
                    return ServerResponse.createByErrorMessage("增加产品失败");
                }
            }
        }

        return ServerResponse.createByErrorMessage("新增或更新产品参数不正确");
    }

    /**
     * 产品上下架
     * @param productId :产品id
     * @param status    :产品上下架状态
     * @return  :响应体
     */
    public ServerResponse setSaleStatus(Integer productId,Integer status){
        //判断传来的参数是否有效
        if(productId == null || status == null){
            return ServerResponse.createByErrorCodeMessage(ResponseCode.ILLEGAL_ARGUMENT.getCode(),ResponseCode.ILLEGAL_ARGUMENT.getDesc());
        }
        Product product = new Product();
        product.setId(productId);
        product.setStatus(status);
        //根据产品主键，选择性更新
        int rowCount = productMapper.updateByPrimaryKeySelective(product);
        //操作成功
        if(rowCount > 0){
            return ServerResponse.createBySuccess("修改产品销售状态成功");
        }
        //操作失败
        return ServerResponse.createByErrorMessage("修改产品销售状态失败");

    }

    /**
     * 获取商品详情
     * @param productId :商品id
     * @return
     */
    public ServerResponse<ProductDetailVo> manageProductDetail(Integer productId){
        if(productId == null){
            return ServerResponse.createByErrorCodeMessage(ResponseCode.ILLEGAL_ARGUMENT.getCode(),ResponseCode.ILLEGAL_ARGUMENT.getDesc());
        }

        Product product = productMapper.selectByPrimaryKey(productId);
        if(product == null){
            return ServerResponse.createByErrorMessage("产品已下架或删除");
        }

        //VO对象 -- value object
        //pojo -> bo(business object) -> vo(view object)
        ProductDetailVo productDetailVo = assembleProductDetailVo(product);
        return ServerResponse.createBySuccess(productDetailVo);
    }

    /**
     * 通过product把productDetailVo组装上
     * @param product
     * @return
     */
    private ProductDetailVo assembleProductDetailVo(Product product){
        ProductDetailVo productDetailVo = new ProductDetailVo();
        productDetailVo.setId(product.getId());
        productDetailVo.setSubtitle(product.getSubtitle());
        productDetailVo.setPrice(product.getPrice());
        productDetailVo.setMainImage(product.getMainImage());
        productDetailVo.setSubImages(product.getSubImages());
        productDetailVo.setCategoryId(product.getCategoryId());
        productDetailVo.setDetail(product.getDetail());
        productDetailVo.setName(product.getName());
        productDetailVo.setStatus(product.getStatus());
        productDetailVo.setStock(product.getStock());

        productDetailVo.setImageHost(PropertiesUtil.getProperty("ftp.server.http.prefix","http://image.shopping.com/"));

        Category category = categoryMapper.selectByPrimaryKey(product.getCategoryId());
        if(category == null){
            productDetailVo.setParentCategoryId(0);//默认根节点
        }else{
            productDetailVo.setParentCategoryId(category.getParentId());
        }

        productDetailVo.setCreateTime(DateTimeUtil.dateToStr(product.getCreateTime()));
        productDetailVo.setUpdateTime(DateTimeUtil.dateToStr(product.getUpdateTime()));

        return productDetailVo;
    }
}
