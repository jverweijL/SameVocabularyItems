package com.liferay.demo.infolistprovider;


import com.liferay.asset.kernel.model.AssetCategory;
import com.liferay.asset.kernel.model.AssetEntry;
import com.liferay.asset.kernel.service.AssetCategoryLocalService;
import com.liferay.asset.util.AssetHelper;
import com.liferay.document.library.kernel.model.DLFileEntry;
import com.liferay.info.list.provider.InfoListProvider;
import com.liferay.info.list.provider.InfoListProviderContext;
import com.liferay.info.pagination.Pagination;
import com.liferay.info.sort.Sort;
import com.liferay.portal.kernel.language.LanguageUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.Company;
import com.liferay.portal.kernel.model.Group;
import com.liferay.portal.kernel.model.Layout;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.search.*;
import com.liferay.portal.kernel.search.generic.BooleanQueryImpl;
import com.liferay.portal.kernel.util.ResourceBundleLoader;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import java.util.*;

/**
 * @author jverweij
 */
@Component(
	immediate = true,
	property = {
		// TODO enter required service properties
	},
	service = InfoListProvider.class
)
public class SameVocabularyAssetsInfoListProvider implements InfoListProvider<AssetEntry> {

	@Override
	public List<AssetEntry> getInfoList(
			InfoListProviderContext infoListProviderContext) {
		Pagination pagination = Pagination.of(10,0);

		return getInfoList(infoListProviderContext, pagination, null);
	}

	@Override
	public List<AssetEntry> getInfoList(
			InfoListProviderContext infoListProviderContext, Pagination pagination,
			Sort sort) {

		try {
			SearchContext searchContext = _getSearchContext(infoListProviderContext);
			searchContext.setStart(pagination.getStart());
			searchContext.setEnd(pagination.getEnd());

			SearchEngine searchEngine = SearchEngineHelperUtil.getSearchEngine(SearchEngineHelperUtil.getDefaultSearchEngineId());
			IndexSearcher searcher = searchEngine.getIndexSearcher();
			Hits hits = searcher.search(searchContext,getSearchQuery(infoListProviderContext));

			List<AssetEntry> assets = _assetHelper.getAssetEntries(hits);

			Iterator<AssetEntry> entries = assets.iterator();
			while (entries.hasNext()) {
				AssetEntry entry = entries.next();
				System.out.println(entry.getClassName() + "|" + entry.getClassPK() + "|" + entry.getEntryId());
			}

			return assets;
		}
		catch (Exception exception) {
			_log.error("Unable to get asset entries", exception);
		}

		return Collections.emptyList();
	}

	@Override
	public int getInfoListCount(
			InfoListProviderContext infoListProviderContext) {

		try {
			SearchContext searchContext = _getSearchContext(infoListProviderContext);

			SearchEngine searchEngine = SearchEngineHelperUtil.getSearchEngine(SearchEngineHelperUtil.getDefaultSearchEngineId());
			IndexSearcher searcher = searchEngine.getIndexSearcher();
			Long count = searcher.searchCount(searchContext,getSearchQuery(infoListProviderContext));

			return count.intValue();
		}
		catch (Exception exception) {
			_log.error("Unable to get search count", exception);
		}

		return 0;
	}

	@Override
	public String getLabel(Locale locale) {
		ResourceBundle resourceBundle =
				_resourceBundleLoader.loadResourceBundle(locale);

		return LanguageUtil.get(resourceBundle, "same-vocabulary-content");
	}

	private SearchContext _getSearchContext(
			InfoListProviderContext infoListProviderContext)
			throws Exception {

		Company company = infoListProviderContext.getCompany();

		long groupId = company.getGroupId();

		Optional<Group> groupOptional =
				infoListProviderContext.getGroupOptional();

		if (groupOptional.isPresent()) {
			Group group = groupOptional.get();

			groupId = group.getGroupId();
		}

		User user = infoListProviderContext.getUser();

		Optional<Layout> layoutOptional =
				infoListProviderContext.getLayoutOptional();

		SearchContext searchContext = SearchContextFactory.getInstance(
				new long[0], new String[0], new HashMap<>(), company.getCompanyId(),
				null, layoutOptional.orElse(null), null, groupId, null,
				user.getUserId());

		searchContext.setSorts(
				SortFactoryUtil.create(
						Field.MODIFIED_DATE,
						com.liferay.portal.kernel.search.Sort.LONG_TYPE, true),
				SortFactoryUtil.create(
						Field.CREATE_DATE,
						com.liferay.portal.kernel.search.Sort.LONG_TYPE, true));

		return searchContext;
	}

	protected BooleanQuery getSearchQuery(InfoListProviderContext infoListProviderContext){

		BooleanQuery searchQuery = new BooleanQueryImpl();
		BooleanQueryImpl categoriesQuery = new BooleanQueryImpl();

		try {
			// TODO make this configurable
			// filter only latest version
			//searchQuery.addRequiredTerm("head", Boolean.TRUE);

			// TODO make this configurable
			// filter on type of asset
			searchQuery.addRequiredTerm(Field.ENTRY_CLASS_NAME, DLFileEntry.class.getName());

			// TODO limit to specific vocabularyId and/or parentId
			// category=A OR category=B OR category=C
			User user = infoListProviderContext.getUser();
			List<AssetCategory> categories = _AssetCategoryLocalService.getCategories(User.class.getName(), user.getPrimaryKey());
			for (AssetCategory category : categories) {
				System.out.println("category: " + category.getName() + "|" + category.getCategoryId());
				categoriesQuery.addTerm(Field.ASSET_CATEGORY_IDS, Long.valueOf(category.getCategoryId()).toString(), Boolean.FALSE);
			}
			searchQuery.add(categoriesQuery, BooleanClauseOccur.MUST);
		} catch(Exception e) {}
		return searchQuery;
	}

	//@Reference
//	protected Portal portal;

	private static final Log _log = LogFactoryUtil.getLog(
			SameVocabularyAssetsInfoListProvider.class);

	@Reference
	private AssetHelper _assetHelper;

	@Reference
	private AssetCategoryLocalService _AssetCategoryLocalService;

	@Reference(target = "(bundle.symbolic.name=com.liferay.asset.service)")
	private ResourceBundleLoader _resourceBundleLoader;

	//@Reference
	//protected Queries queries;
}