/*
 * $Id: ApplicationCategoryCreator.java,v 1.16 2008/08/07 13:43:35 valdas Exp $ Created on
 * Jan 12, 2006
 * 
 * Copyright (C) 2006 Idega Software hf. All Rights Reserved.
 * 
 * This software is the proprietary information of Idega hf. Use is subject to
 * license terms.
 */
package is.idega.idegaweb.egov.application.presentation;

import is.idega.idegaweb.egov.application.data.Application;
import is.idega.idegaweb.egov.application.data.ApplicationCategory;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import java.io.Serializable;

import javax.ejb.FinderException;

import com.idega.block.text.data.LocalizedText;
import com.idega.block.text.data.LocalizedTextHome;
import com.idega.builder.business.BuilderLogic;
import com.idega.core.cache.IWCacheManager2;
import com.idega.core.localisation.business.ICLocaleBusiness;
import com.idega.core.localisation.data.ICLanguage;
import com.idega.core.localisation.data.ICLanguageHome;
import com.idega.core.localisation.data.ICLocale;
import com.idega.data.IDOLookup;
import com.idega.idegaweb.IWBundle;
import com.idega.idegaweb.IWCacheManager;
import com.idega.idegaweb.IWResourceBundle;
import com.idega.presentation.IWContext;
import com.idega.presentation.Layer;
import com.idega.presentation.Table2;
import com.idega.presentation.TableCell2;
import com.idega.presentation.TableRow;
import com.idega.presentation.TableRowGroup;
import com.idega.presentation.text.Link;
import com.idega.presentation.text.Text;
import com.idega.presentation.ui.Form;
import com.idega.presentation.ui.Label;
import com.idega.presentation.ui.SubmitButton;
import com.idega.presentation.ui.TextArea;
import com.idega.presentation.ui.TextInput;

public class ApplicationCategoryCreator extends ApplicationBlock {
	
	public static final String PARAMETER_ACTION = "prm_action";
	public static final String ACTION_CREATE = "create";
	public static final String ACTION_EDIT = "edit";
	public static final String ACTION_SAVE = "save";
	public static final String ACTION_CATEGORY_UP = "category_up";
	public static final String ACTION_CATEGORY_DOWN = "category_down";
	public static final String ACTION_APP_UP = "app_up";
	public static final String ACTION_APP_DOWN = "app_down";
	public static final String ACTION_DELETE = "delete";
	public static final String ACTION_LIST = "list";

	private IWResourceBundle iwrb;
	private IWBundle iwb;
	
	private void clearApplicationCategoryViewerCache(IWContext iwc) {
		IWCacheManager2 cacheManager = IWCacheManager2.getInstance(iwc.getIWMainApplication());
		Map<Serializable, ?> cache = cacheManager.getCache(ApplicationCategoryViewer.CACHE_KEY);
		if(cache != null) {
			cache.clear();
		}
		BuilderLogic.getInstance().clearAllCaches();
		IWCacheManager.getInstance(iwc.getIWMainApplication()).clearAllCaches();
	}

	@Override
	public void present(IWContext iwc) throws Exception {
		this.iwrb = getResourceBundle(iwc);
		this.iwb = getBundle(iwc);
		
		List<ICLocale> locales = ICLocaleBusiness.listOfLocales();
		
		String action = iwc.getParameter(PARAMETER_ACTION);
		if (ACTION_CREATE.equals(action)) {
			getCategoryCreationForm(iwc, null, locales);
		}
		else if (ACTION_EDIT.equals(action)) {
			ApplicationCategory category = getApplicationBusiness(iwc).getApplicationCategoryHome().findByPrimaryKey(Integer.parseInt(iwc.getParameter("id")));
			getCategoryCreationForm(iwc, category, locales);
		}
		else if (ACTION_SAVE.equals(action)) {
			String id = iwc.getParameter("id");
			String name = iwc.getParameter("name");
			String desc = iwc.getParameter("desc");
			String priority = iwc.getParameter("priority");
			
			
			
			if (name != null && !name.trim().equals("")) {
				ApplicationCategory cat = null;
				if (id != null) {
					try {
						cat = getApplicationBusiness(iwc).getApplicationCategoryHome().findByPrimaryKey(
								new Integer(iwc.getParameter("id")));
					}
					catch (FinderException f) {
						f.printStackTrace();
					}
				}
				else {
					cat = getApplicationBusiness(iwc).getApplicationCategoryHome().create();
				}
				
				cat.setName(name);
				cat.setDescription(desc);
				if(priority != null && !priority.equals("")) {
					cat.setPriority(new Integer(priority));
				}
				cat.store();
				
				for(Iterator<ICLocale> it = locales.iterator(); it.hasNext(); ) {
					ICLocale locale = it.next();
					String locName = iwc.getParameter(locale.getName() + "_locale");
					
					if(locName != null && !locName.equals("")) {
						LocalizedText locText = cat.getLocalizedText(locale.getLocaleID());
						boolean newText = false;
						if(locText == null) {
							locText = getLocalizedTextHome().create();
							newText = true;
						}
						
						locText.setLocaleId(locale.getLocaleID());
						locText.setBody(locName);
						
						locText.store();
						
						if(newText) {
							cat.addLocalizedName(locText);
							
							
						}
						
					}
				}
				
				IWCacheManager.getInstance(iwc.getIWMainApplication()).invalidateCache(ApplicationCategoryViewer.CACHE_KEY);
				IWCacheManager.getInstance(iwc.getIWMainApplication()).invalidateCache(ApplicationFavorites.CACHE_KEY);
			}
			listExisting(iwc);
		}
		else if(ACTION_CATEGORY_UP.equals(action)) {
			String id = iwc.getParameter("id");
			
			ApplicationCategory cat = null;
			if (id != null) {
				try {
					cat = getApplicationBusiness(iwc).getApplicationCategoryHome().findByPrimaryKey(
							new Integer(id));
				}
				catch (FinderException f) {
					f.printStackTrace();
				}
			}
			Integer priority = cat.getPriority();
			ApplicationCategory upperCat = null;
			if(priority != null) {
				try {
					upperCat = getApplicationBusiness(iwc).getApplicationCategoryHome().findByPriority(priority.intValue() - 1);
					upperCat.setPriority(-1);
					upperCat.store();
				}
				catch (FinderException f) {
					f.printStackTrace();
				}
				cat.setPriority(priority.intValue() - 1);
			}
			
			cat.store();
			
			if(upperCat != null) {
				upperCat.setPriority(priority.intValue());
				upperCat.store();
			}
			
			clearApplicationCategoryViewerCache(iwc);
			
			listExisting(iwc);
		}
		else if(ACTION_CATEGORY_DOWN.equals(action)) {
			String id = iwc.getParameter("id");
			
			ApplicationCategory cat = null;
			if (id != null) {
				try {
					cat = getApplicationBusiness(iwc).getApplicationCategoryHome().findByPrimaryKey(
							new Integer(id));
				}
				catch (FinderException f) {
					f.printStackTrace();
				}
			}
			Integer priority = cat.getPriority();
			ApplicationCategory lowerCat = null;
			if(priority != null) {
				try {
					lowerCat = getApplicationBusiness(iwc).getApplicationCategoryHome().findByPriority(priority.intValue() + 1);
					lowerCat.setPriority(-1);
					lowerCat.store();
				}
				catch (FinderException f) {
					f.printStackTrace();
				}
				cat.setPriority(priority.intValue() + 1);
			}
			
			cat.store();
			
			if(lowerCat != null) {
				lowerCat.setPriority(priority.intValue());
				lowerCat.store();
			}
			
			clearApplicationCategoryViewerCache(iwc);
			
			listExisting(iwc);
		}
		else if(ACTION_APP_UP.equals(action)) {
			String appId = iwc.getParameter("app_id");
			String id = iwc.getParameter("id");
			
			Application app = null;
			ApplicationCategory category = null;
			if (appId != null) {
				try {
					app = getApplicationBusiness(iwc).getApplicationHome().findByPrimaryKey(
							new Integer(appId));
				}
				catch (FinderException f) {
					f.printStackTrace();
				}
			}
			Integer priority = app.getPriority();
			Application upperApp = null;
			if(priority != null) {
				try {
					category = getApplicationBusiness(iwc).getApplicationCategoryHome().findByPrimaryKey(new Integer(id));
					
					upperApp = getApplicationBusiness(iwc).getApplicationHome().findByCategoryAndPriority(category, priority.intValue() - 1);
					upperApp.setPriority(-1);
					upperApp.store();
				}
				catch (FinderException f) {
					f.printStackTrace();
				}
				app.setPriority(priority.intValue() - 1);
			}
			
			app.store();
			
			if(upperApp != null) {
				upperApp.setPriority(priority.intValue());
				upperApp.store();
			}
			
			clearApplicationCategoryViewerCache(iwc);
			
			getCategoryCreationForm(iwc, category, locales);
		}
		else if(ACTION_APP_DOWN.equals(action)) {
			String appId = iwc.getParameter("app_id");
			String id = iwc.getParameter("id");
			
			Application app = null;
			ApplicationCategory category = null;
			if (appId != null) {
				try {
					app = getApplicationBusiness(iwc).getApplicationHome().findByPrimaryKey(
							new Integer(appId));
				}
				catch (FinderException f) {
					f.printStackTrace();
				}
			}
			Integer priority = app.getPriority();
			Application lowerApp = null;
			if(priority != null) {
				try {
					category = getApplicationBusiness(iwc).getApplicationCategoryHome().findByPrimaryKey(new Integer(id));
							
					lowerApp = getApplicationBusiness(iwc).getApplicationHome().findByCategoryAndPriority(category, priority.intValue() + 1);
					lowerApp.setPriority(-1);
					lowerApp.store();
				}
				catch (FinderException f) {
					f.printStackTrace();
				}
				app.setPriority(priority.intValue() + 1);
			}
			
			app.store();
			
			if(lowerApp != null) {
				lowerApp.setPriority(priority.intValue());
				lowerApp.store();
			}
			
			clearApplicationCategoryViewerCache(iwc);
			
			getCategoryCreationForm(iwc, category, locales);
		}
		else if (ACTION_DELETE.equals(action)) {
			try {
				ApplicationCategory cat = getApplicationBusiness(iwc).getApplicationCategoryHome().findByPrimaryKey(
						new Integer(iwc.getParameter("id")));
				cat.remove();
			}
			catch (FinderException f) {
				f.printStackTrace();
			}
			listExisting(iwc);
		}
		else if (ACTION_LIST.equals(action)) {
			listExisting(iwc);
		}
		else {
			listExisting(iwc);
		}
	}

	/**
	 * <p>
	 * this method creates the form for creating new categories as well as editing the properties of existing ones, plus it allows to see all the applications
	 * within this category and change their display ordering
	 * </p>
	 * 
	 * @throws RemoteException
	 */
	private void getCategoryCreationForm(IWContext iwc, ApplicationCategory cat, List<ICLocale> locales) throws RemoteException {
		if(cat != null) {
			getApplicationBusiness(iwc).checkApplicationPriorityConstraint(cat);
		}
		
		Form form = new Form();
		form.setID("applicationCategoryCreator");
		form.setStyleClass("adminForm");
		
		TextInput tName = new TextInput("name");
		TextArea tDesc = new TextArea("desc");
		
		if (cat != null) {
			tName.setContent(cat.getName());
			tDesc.setContent(cat.getDescription());
			form.addParameter("id", cat.getPrimaryKey().toString());
		}

		Layer layer = new Layer(Layer.DIV);
		layer.setStyleClass("formSection");
		form.add(layer);
		
		Layer formItem = new Layer(Layer.DIV);
		formItem.setStyleClass("formItem");
		Label label = new Label(this.iwrb.getLocalizedString("default_name", "Default name"), tName);
		formItem.add(label);
		formItem.add(tName);
		layer.add(formItem);

		formItem = new Layer(Layer.DIV);
		formItem.setStyleClass("formItem");
		label = new Label(this.iwrb.getLocalizedString("default_description", "Default description"), tDesc);
		formItem.add(label);
		formItem.add(tDesc);
		layer.add(formItem);
		
		for(Iterator<ICLocale> it = locales.iterator(); it.hasNext(); ) {
			ICLocale locale = it.next();
			Locale javaLocale = ICLocaleBusiness.getLocaleFromLocaleString(locale.getLocale());
			
			TextInput locInput = new TextInput(locale.getName() + "_locale");
			if(cat != null) {
				LocalizedText text = cat.getLocalizedText(locale.getLocaleID());
				locInput.setValue(text == null ? "" : text.getBody());
			}
			formItem = new Layer(Layer.DIV);
			formItem.setStyleClass("formItem");
			label = new Label(javaLocale.getDisplayLanguage(), locInput);
			formItem.add(label);
			formItem.add(locInput);
			layer.add(formItem);
		}
		
		List apps = null;
		if(cat != null) {
			try {
				apps = new ArrayList(getApplicationBusiness(iwc).getApplicationHome().findAllByCategoryOrderedByPriority(cat));
			}
			catch (FinderException f) {
				f.printStackTrace();
			}
		}
		
		if(apps != null && !apps.isEmpty()) {
			Table2 table = new Table2();
			table.setWidth("100%");
			table.setCellpadding(0);
			table.setCellspacing(0);
			table.setStyleClass("ruler");
			table.setStyleClass("adminTable");
			
			TableRowGroup group = table.createHeaderRowGroup();
			TableRow row = group.createRow();
			TableCell2 cell = row.createHeaderCell();
			cell.setStyleClass("firstColumn");
			cell.setStyleClass("application");
			cell.add(new Text(this.iwrb.getLocalizedString("application", "Application")));
			
			cell = row.createHeaderCell();
			cell.setStyleClass("description");
			cell.add(new Text(this.iwrb.getLocalizedString("priority", "Priority")));
			
			group = table.createBodyRowGroup();
			int iRow = 1;
			
			Iterator iter = apps.iterator();
			
			while (iter.hasNext()) {
				Application app = (Application) iter.next();
				
				row = table.createRow();
				
				if (iRow % 2 == 0) {
					row.setStyleClass("evenRow");
				}
				else {
					row.setStyleClass("oddRow");
				}

				cell = row.createCell();
				cell.setStyleClass("firstColumn");
				cell.setStyleClass("application");
				cell.add(new Text(app.getName()));
				
				cell = row.createCell();
				cell.setStyleClass("description");
				cell.setStyleClass("lastColumn");
				
				Link up = new Link(this.iwb.getImage("previous.png", this.iwrb.getLocalizedString("previous", "Up")));
				up.addParameter(PARAMETER_ACTION, ACTION_APP_UP);
				up.addParameter("app_id", app.getPrimaryKey().toString());
				up.addParameter("id", cat.getPrimaryKey().toString());
				up.setStyleClass("flippedImageLink");
				cell.add(up);
				
				if (iRow <= 1) {
					up.setStyleAttribute("visibility", "hidden");
				}
				
				Link down = new Link(this.iwb.getImage("next.png", this.iwrb.getLocalizedString("next", "Down")));
				down.addParameter(PARAMETER_ACTION, ACTION_APP_DOWN);
				down.addParameter("app_id", app.getPrimaryKey().toString());
				down.addParameter("id", cat.getPrimaryKey().toString());
				down.setStyleClass("flippedImageLink");
				cell.add(down);
				
				if (iRow >= apps.size()) {
					down.setStyleAttribute("visibility", "hidden");
				}	

				iRow++;
			}
			
			Layer clearLayer = new Layer(Layer.DIV);
			clearLayer.setStyleClass("Clear");
			layer.add(clearLayer);
			
			form.add(table);
		}
		
		Layer buttonLayer = new Layer(Layer.DIV);
		buttonLayer.setStyleClass("buttonLayer");
		form.add(buttonLayer);
		
		SubmitButton back = new SubmitButton(this.iwrb.getLocalizedString("back", "Back"), PARAMETER_ACTION, "list");
		buttonLayer.add(back);
		
		SubmitButton save = new SubmitButton(this.iwrb.getLocalizedString("save", "Save"), PARAMETER_ACTION, "save");
		buttonLayer.add(save);

		add(form);
	}
	
	protected ICLanguageHome getICLanguageHome() throws RemoteException {
		return (ICLanguageHome) IDOLookup.getHome(ICLanguage.class);
	}
	
	protected LocalizedTextHome getLocalizedTextHome() throws RemoteException {
		return (LocalizedTextHome) IDOLookup.getHome(LocalizedText.class);
	}

	private void listExisting(IWContext iwc) throws FinderException, RemoteException {
		getApplicationBusiness(iwc).checkApplicationCategoryPriorityConstraint();
		Collection categories = getApplicationBusiness(iwc).getApplicationCategoryHome().findAllOrderedByPriority();
		
		Form form = new Form();
		form.setID("applicationCategoryCreator");
		form.setStyleClass("adminForm");
		
		Table2 table = new Table2();
		table.setWidth("100%");
		table.setCellpadding(0);
		table.setCellspacing(0);
		table.setStyleClass("ruler");
		table.setStyleClass("adminTable");
		form.add(table);
		
		TableRowGroup group = table.createHeaderRowGroup();
		TableRow row = group.createRow();
		TableCell2 cell = row.createHeaderCell();
		cell.setStyleClass("firstColumn");
		cell.setStyleClass("category");
		cell.add(new Text(this.iwrb.getLocalizedString("category", "Category")));
		
		cell = row.createHeaderCell();
		cell.setStyleClass("description");
		cell.add(new Text(this.iwrb.getLocalizedString("description", "Description")));
		
		cell = row.createHeaderCell();
		cell.setStyleClass("description");
		cell.add(new Text(this.iwrb.getLocalizedString("priority", "Priority")));
		
		cell = row.createHeaderCell();
		cell.setStyleClass("edit");
		cell.add(Text.getNonBrakingSpace());

		cell = row.createHeaderCell();
		cell.setStyleClass("remove");
		cell.setStyleClass("lastColumn");
		cell.add(Text.getNonBrakingSpace());

		group = table.createBodyRowGroup();
		int iRow = 1;
		
		Iterator iter = categories.iterator();
		while (iter.hasNext()) {
			ApplicationCategory cat = (ApplicationCategory) iter.next();
			row = table.createRow();
			
			Link edit = new Link(this.iwb.getImage("edit.png", this.iwrb.getLocalizedString("edit", "Edit")));
			edit.addParameter(PARAMETER_ACTION, ACTION_EDIT);
			edit.addParameter("id", cat.getPrimaryKey().toString());
			
			Link delete = new Link(this.iwb.getImage("delete.png", this.iwrb.getLocalizedString("remove", "Remove")));
			delete.addParameter(PARAMETER_ACTION, ACTION_DELETE);
			delete.addParameter("id", cat.getPrimaryKey().toString());

			if (iRow % 2 == 0) {
				row.setStyleClass("evenRow");
			}
			else {
				row.setStyleClass("oddRow");
			}

			cell = row.createCell();
			cell.setStyleClass("firstColumn");
			cell.setStyleClass("category");
			cell.add(new Text(cat.getName()));

			cell = row.createCell();
			cell.setStyleClass("description");
			if (cat.getDescription() != null) {
				String description = cat.getDescription();
				if (description.length() > 35) {
					description = description.substring(0, 35) + "...";
				}
				cell.add(new Text(description));
			}
			else {
				cell.add(new Text(Text.NON_BREAKING_SPACE));
			}
			
			cell = row.createCell();
			cell.setStyleClass("description");
			
			Link up = new Link(this.iwb.getImage("previous.png", this.iwrb.getLocalizedString("previous", "Up")));
			up.addParameter(PARAMETER_ACTION, ACTION_CATEGORY_UP);
			up.addParameter("id", cat.getPrimaryKey().toString());
			up.setStyleClass("flippedImageLink");
			cell.add(up);
			
			if(iRow <= 1) {
				
				up.setStyleAttribute("visibility", "hidden");
			
			}
			
			Link down = new Link(this.iwb.getImage("next.png", this.iwrb.getLocalizedString("next", "Down")));
			down.addParameter(PARAMETER_ACTION, ACTION_CATEGORY_DOWN);
			down.addParameter("id", cat.getPrimaryKey().toString());
			down.setStyleClass("flippedImageLink");
			cell.add(down);
			
			if(iRow >= categories.size()) {
				
				down.setStyleAttribute("visibility", "hidden");
			
			}	

			cell = row.createCell();
			cell.setStyleClass("edit");
			cell.add(edit);

			cell = row.createCell();
			cell.setStyleClass("lastColumn");
			cell.setStyleClass("remove");
			cell.add(delete);

			iRow++;
		}

		Layer buttonLayer = new Layer(Layer.DIV);
		buttonLayer.setStyleClass("buttonLayer");
		form.add(buttonLayer);
		
		SubmitButton newLink = new SubmitButton(this.iwrb.getLocalizedString("new_category", "New Category"), PARAMETER_ACTION, ACTION_CREATE);
		buttonLayer.add(newLink);
		
		add(form);
	}
}