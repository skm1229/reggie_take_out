package com.skm.reggie.dto;

import com.skm.reggie.domain.Setmeal;
import com.skm.reggie.domain.SetmealDish;
import lombok.Data;
import java.util.List;

@Data
public class SetmealDto extends Setmeal {

    private List<SetmealDish> setmealDishes;

    private String categoryName;
}
