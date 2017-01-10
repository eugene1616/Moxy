package com.arellomobile.mvp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.arellomobile.mvp.presenter.PresenterField;
import com.arellomobile.mvp.presenter.PresenterType;

/**
 * Date: 18-Dec-15
 * Time: 13:16
 *
 * @author Yuri Shmakov
 * @author Alexander Blinov
 */
public class MvpProcessor {
	public static final String PRESENTER_BINDER_SUFFIX = "$$PresentersBinder";
	public static final String PRESENTER_BINDER_INNER_SUFFIX = "Binder";
	public static final String VIEW_STATE_SUFFIX = "$$State";
	public static final String VIEW_STATE_PROVIDER_SUFFIX = "$$ViewStateProvider";

	/**
	 * <p>1) Generates tag for identification MvpPresenter</p>
	 * <p>2) Checks if presenter with tag is already exist in {@link com.arellomobile.mvp.PresenterStore}, and returns it</p>
	 * <p>3) If {@link com.arellomobile.mvp.PresenterStore} doesn't contain MvpPresenter with current tag, {@link PresenterField} will create it</p>
	 *
	 * @param <Delegated>    type of delegated
	 * @param target         object that want injection
	 * @param presenterField info about presenter from {@link com.arellomobile.mvp.presenter.InjectPresenter}
	 * @param delegateTag    unique tag generated by {@link MvpDelegate#generateTag()}   @return MvpPresenter instance
	 */
	private <Delegated> MvpPresenter<? super Delegated> getMvpPresenter(Delegated target, PresenterField<?, ? super Delegated> presenterField, String delegateTag) {
		Class<? extends MvpPresenter<?>> presenterClass = presenterField.getPresenterClass();
		PresenterStore presenterStore = MvpFacade.getInstance().getPresenterStore();

		PresenterType type = presenterField.getPresenterType();
		String tag;
		if (type == PresenterType.LOCAL) {
			tag = delegateTag + "$" + presenterField.getTag(target);
		} else {
			tag = presenterField.getTag(target);
		}

		//noinspection unchecked
		MvpPresenter<? super Delegated> presenter = presenterStore.get(type, tag, presenterClass);
		if (presenter != null) {
			return presenter;
		}

		//noinspection unchecked
		presenter = (MvpPresenter<? super Delegated>) presenterField.providePresenter(target);

		if (presenter == null) {
			return null;
		}

		presenter.setPresenterType(type);
		presenter.setTag(tag);
		presenter.setPresenterClass(presenterClass);
		presenterStore.add(type, tag, presenterClass, presenter);

		return presenter;
	}


	/**
	 * <p>Gets presenters {@link java.util.List} annotated with {@link com.arellomobile.mvp.presenter.InjectPresenter} for view.</p>
	 * <p>See full info about getting presenter instance in {@link #getMvpPresenter}</p>
	 *
	 * @param delegated   class contains presenter
	 * @param delegateTag unique tag generated by {@link MvpDelegate#generateTag()}
	 * @param <Delegated> type of delegated
	 * @return presenters list for specifies presenters container
	 */
	<Delegated> List<MvpPresenter<? super Delegated>> getMvpPresenters(Delegated delegated, String delegateTag) {
		@SuppressWarnings("unchecked")
		Class<? super Delegated> aClass = (Class<Delegated>) delegated.getClass();
		List<Object> presenterBinders = null;

		while (aClass != Object.class && presenterBinders == null) {
			presenterBinders = MoxyReflector.getPresenterBinders(aClass);

			aClass = aClass.getSuperclass();
		}

		if (presenterBinders == null || presenterBinders.isEmpty()) {
			return Collections.emptyList();
		}

		List<MvpPresenter<? super Delegated>> presenters = new ArrayList<>();
		PresentersCounter presentersCounter = MvpFacade.getInstance().getPresentersCounter();
		for (Object presenterBinderObject : presenterBinders) {
			//noinspection unchecked
			PresenterBinder<? super Delegated> presenterBinder = (PresenterBinder<? super Delegated>) presenterBinderObject;
			List<? extends PresenterField<?, ? super Delegated>> presenterFields = presenterBinder.getPresenterFields();

			for (PresenterField<?, ? super Delegated> presenterField : presenterFields) {
				MvpPresenter<? super Delegated> presenter = getMvpPresenter(delegated, presenterField, delegateTag);

				if (presenter != null) {
					presentersCounter.injectPresenter(presenter, delegateTag);
					presenters.add(presenter);
					presenterField.bind(delegated, presenter);
				}
			}
		}

		return presenters;
	}
}
