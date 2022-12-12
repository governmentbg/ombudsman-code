/*
 * Copyright 2009-2014 PrimeTek.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ib.omb.beans;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.PostConstruct;
import javax.enterprise.context.SessionScoped;
import javax.inject.Named;

import org.primefaces.PrimeFaces;

import com.ib.indexui.utils.JSFUtils;

@Named
@SessionScoped
public class GuestPreferences implements Serializable {

    /**
	 * 
	 */
	private static final long serialVersionUID = 5644709981503440640L;
	private static final String COLOR = "purple";

	private String layoutPrimaryColor = COLOR;

    private String componentTheme = "index-" + COLOR;
    
    private String menuTheme = COLOR;
    
    private String menuColor = "light";
    
    private String inputStyle = "outlined";

    private boolean horizontalMenu = true;
    
    private boolean darkMode;

    private List<ComponentTheme> componentThemes = new ArrayList<>();
    
    private Map<String, List<MenuTheme>> menuColors = new HashMap<>();
    
    private String containerPosition = ""; //auto
    
    @PostConstruct
    public void init() {        
        
        //------------------------------------------------------------------------
        // изчитане от бисквитка какви за били избраните настройки от потребителят 
        String val = JSFUtils.readCookie("user_theme_mod");
        darkMode =  val.equals("true") ? true : false;

        val = JSFUtils.readCookie("user_menu_mod");
        horizontalMenu = val.equals("true") ? true : horizontalMenu;

        val = JSFUtils.readCookie("user_menu_theme");
        menuTheme = val.isEmpty() ? menuTheme : val;

        val = JSFUtils.readCookie("user_component_theme");
        componentTheme = val.isEmpty() ? componentTheme : val;

        val = JSFUtils.readCookie("user_menu_color");
        menuColor = val.isEmpty() ? menuColor : val;
        
         //------------------------------------------------------------------------

        componentThemes.add(new ComponentTheme("Purple", "index-purple", "#B48EAD"));
        
        /************** Light ********************/
        List<MenuTheme> menuThemes = new ArrayList<>();
        menuThemes.add(new MenuTheme("Purple", "purple", "#B48EAD"));
        menuColors.put("light", menuThemes);
       
        /************** Dark ********************/
        menuThemes = new ArrayList<>();
        menuThemes.add(new MenuTheme("Purple", "purple", "#B48EAD"));
        menuColors.put("dark", menuThemes);
        
    }
    /*
    public void onMenuTypeChange() {
        PrimeFaces.current().executeScript("PrimeFaces.MirageConfigurator.changeMenuToHorizontal(" + horizontalMenu +")");
    }

    public void onColorSchemeChange() {
        PrimeFaces.current().executeScript("PrimeFaces.MirageConfigurator.changeLayout('" + layoutPrimaryColor + "', '" + componentTheme + "', " + darkMode + ")");
    }

    public void onMenuColorChange() {
        PrimeFaces.current().executeScript("PrimeFaces.MirageConfigurator.changeMenuTheme('" + menuColor + "', '" + menuTheme + "', " + darkMode + ")");
    }

    public String getInputStyleClass() {
        return this.inputStyle.equals("filled") ? "ui-input-filled" : "";
    }
     */
    public String getInputStyle() {
        return inputStyle;
    }

    public void setInputStyle(String inputStyle) {
        this.inputStyle = inputStyle;
    }
    
    public String getLayoutConfig() {
        StringBuilder sb = new StringBuilder();
        String color = "";
        if ("custom".equals(this.menuColor)) {
            color = this.menuTheme;
        }
        else {
            color = this.isDarkMode() ? "dark" : "light";
        }
        sb.append("layout-menu-").append(color);

        if (this.isHorizontalMenu()) {
            sb.append(" layout-horizontal");
        }

        return sb.toString();
    }

    public String getLayout() {
        return "layout-" + this.layoutPrimaryColor + (this.darkMode ? "-dark" : "-light");
    }
    
    public String getTheme() {
        return this.darkMode ? this.componentTheme + "-dark" : this.componentTheme + "-light";
    }

    public boolean isDarkMode() {
        return darkMode;
    }

    public void setDarkMode(boolean darkMode) {
        this.darkMode = darkMode;
        
        this.menuColor = darkMode ? "dark" : "light";
        this.menuTheme = ((MenuTheme)this.menuColors.get(this.menuColor).get(0)).getFile();
        
    }

    public String getLayoutPrimaryColor() {
        return layoutPrimaryColor;
    }

    public void setLayoutPrimaryColor(String layoutPrimaryColor) {
        this.layoutPrimaryColor = layoutPrimaryColor;
    }

    public String getComponentTheme() {
        return componentTheme;
    }

    public void setComponentTheme(String componentTheme) {
        this.componentTheme = componentTheme;
    }

    public boolean isHorizontalMenu() {
        return horizontalMenu;
    }

    public void setHorizontalMenu(boolean horizontalMenu) {
        this.horizontalMenu = horizontalMenu;
//        if (horizontalMenu) {  // за превключване на позицията на дилтъра дали да е по средата на страницата или не при хоризонталното меню (не се използва за сега но го има заложено по страниците)
//        	containerPosition = "auto";
//        } else {
//        	containerPosition = "";
//        }
    }

    public String getMenuTheme() {
        return menuTheme;
    }

    public void setMenuTheme(String menuTheme) {
        this.menuTheme = menuTheme;
    }

    public List<ComponentTheme> getComponentThemes() {
        return componentThemes;
    }

    public void setComponentThemes(List<ComponentTheme> componentThemes) {
        this.componentThemes = componentThemes;
    }

    public String getMenuColor() {
        return menuColor;
    }

    public void setMenuColor(String menuColor) {
        this.menuColor = menuColor;
        this.menuTheme = ((MenuTheme)this.menuColors.get(menuColor).get(0)).getFile();
    }

    public Map<String, List<MenuTheme>> getMenuColors() {
        return menuColors;
    }

    public void setMenuColors(Map<String, List<MenuTheme>> menuColors) {
        this.menuColors = menuColors;
    }

    public String getContainerPosition() {
		return containerPosition;
	}

	public void setContainerPosition(String containerPosition) {
		this.containerPosition = containerPosition;
	}

	public class MenuTheme {

        String name;
        String file;
        String color;

        public MenuTheme(String name, String file, String color) {
            this.name = name;
            this.file = file;
            this.color = color;
        }

        public String getName() {
            return this.name;
        }

        public String getFile() {
            return this.file;
        }

        public String getColor() {
            return this.color;
        }
    }

    public class ComponentTheme {

        String name;
        String file;
        String color;

        public ComponentTheme(String name, String file, String color) {
            this.name = name;
            this.file = file;
            this.color = color;
        }

        public String getName() {
            return this.name;
        }

        public String getFile() {
            return this.file;
        }

        public String getColor() {
            return this.color;
        }
    }
}
