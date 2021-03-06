package net.osmand.plus.views.mapwidgets;

import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import net.osmand.Location;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.plus.IconsCache;
import net.osmand.plus.MapMarkersHelper;
import net.osmand.plus.MapMarkersHelper.MapMarker;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.dashboard.DashLocationFragment;
import net.osmand.plus.dashboard.DashboardOnMap;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.helpers.MapMarkerDialogHelper;
import net.osmand.plus.views.DirectionDrawable;
import net.osmand.plus.views.OsmandMapLayer.DrawSettings;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.mapwidgets.RouteInfoWidgetsFactory.DistanceToPointInfoControl;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import java.util.List;

public class MapMarkersWidgetsFactory {

	public static final int MIN_DIST_OK_VISIBLE = 40; // meters
	public static final int MIN_DIST_2ND_ROW_SHOW = 150; // meters

	private final MapActivity map;
	private MapMarkersHelper helper;
	private int screenOrientation;
	private boolean portraitMode;
	private boolean largeDevice;

	private View topBar;
	private View addressTopBar;
	private View topBar2nd;
	private View rowView;
	private View rowView2nd;
	private ImageView arrowImg;
	private ImageView arrowImg2nd;
	private TextView distText;
	private TextView distText2nd;
	private TextView addressText;
	private TextView addressText2nd;
	private ImageButton okButton;
	private ImageButton okButton2nd;
	private ImageButton moreButton;
	private ImageButton moreButton2nd;

	private MapMarker marker;
	private MapMarker marker2nd;

	public MapMarkersWidgetsFactory(final MapActivity map) {
		this.map = map;
		helper = map.getMyApplication().getMapMarkersHelper();
		screenOrientation = DashLocationFragment.getScreenOrientation(map);
		portraitMode = AndroidUiHelper.isOrientationPortrait(map);
		largeDevice = AndroidUiHelper.isXLargeDevice(map);

		addressTopBar = map.findViewById(R.id.map_top_bar);
		topBar = map.findViewById(R.id.map_markers_top_bar);
		topBar2nd = map.findViewById(R.id.map_markers_top_bar_2nd);
		rowView = map.findViewById(R.id.map_marker_row);
		rowView2nd = map.findViewById(R.id.map_marker_row_2nd);
		arrowImg = (ImageView) map.findViewById(R.id.map_marker_arrow);
		arrowImg2nd = (ImageView) map.findViewById(R.id.map_marker_arrow_2nd);
		distText = (TextView) map.findViewById(R.id.map_marker_dist);
		distText2nd = (TextView) map.findViewById(R.id.map_marker_dist_2nd);
		addressText = (TextView) map.findViewById(R.id.map_marker_address);
		addressText2nd = (TextView) map.findViewById(R.id.map_marker_address_2nd);
		okButton = (ImageButton) map.findViewById(R.id.marker_btn_ok);
		okButton2nd = (ImageButton) map.findViewById(R.id.marker_btn_ok_2dn);
		moreButton = (ImageButton) map.findViewById(R.id.marker_btn_more);
		moreButton2nd = (ImageButton) map.findViewById(R.id.marker_btn_more_2nd);

		rowView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				showMarkerOnMap(0);
			}
		});
		rowView2nd.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				showMarkerOnMap(1);
			}
		});

		IconsCache iconsCache = map.getMyApplication().getIconsCache();
		if (isLandscapeLayout() && helper.getSortedMapMarkers().size() > 1) {
			moreButton.setVisibility(View.GONE);
		} else {
			moreButton.setImageDrawable(iconsCache.getIcon(R.drawable.ic_overflow_menu_white, R.color.marker_top_2nd_line_color));
			moreButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					MapActivity.clearPrevActivityIntent();
					map.getDashboard().setDashboardVisibility(true, DashboardOnMap.DashboardType.MAP_MARKERS);
				}
			});
		}
		if (moreButton2nd != null) {
			moreButton2nd.setImageDrawable(iconsCache.getIcon(R.drawable.ic_overflow_menu_white, R.color.marker_top_2nd_line_color));
			moreButton2nd.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					MapActivity.clearPrevActivityIntent();
					map.getDashboard().setDashboardVisibility(true, DashboardOnMap.DashboardType.MAP_MARKERS);
				}
			});
		}
		okButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				removeMarker(0);
			}
		});
		okButton2nd.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				removeMarker(1);
			}
		});

		updateVisibility(false);
	}

	private void removeMarker(int index) {
		if (helper.getSortedMapMarkers().size() > index) {
			MapMarker marker = helper.getSortedMapMarkers().get(index);
			helper.removeMapMarker(marker.index);
			helper.addMapMarkerHistory(marker);
		}
	}

	private void showMarkerOnMap(int index) {
		if (helper.getSortedMapMarkers().size() > index) {
			MapMarker marker = helper.getSortedMapMarkers().get(index);
			MapMarkerDialogHelper.showMarkerOnMap(map, marker);
		}
	}

	public boolean updateVisibility(boolean visible) {
		return updateVisibility(topBar, visible);
	}

	public boolean updateVisibility(View v, boolean visible) {
		if (visible != (v.getVisibility() == View.VISIBLE)) {
			if (visible) {
				v.setVisibility(View.VISIBLE);
			} else {
				v.setVisibility(View.GONE);
			}
			v.invalidate();
			return true;
		}
		return false;
	}

	public int getTopBarHeight() {
		return topBar.getHeight();
	}

	public boolean isTopBarVisible() {
		return topBar.getVisibility() == View.VISIBLE
				&& map.findViewById(R.id.MapHudButtonsOverlay).getVisibility() == View.VISIBLE;
	}

	public void updateInfo(LatLon customLocation, int zoom) {
		if (!map.getMyApplication().getSettings().USE_MAP_MARKERS.get()) {
			return;
		}

		List<MapMarker> markers = helper.getSortedMapMarkers();
		if (zoom < 3 || markers.size() == 0
				|| !map.getMyApplication().getSettings().MAP_MARKERS_MODE.get().isToolbar()
				|| map.getMyApplication().getRoutingHelper().isFollowingMode()
				|| map.getMyApplication().getRoutingHelper().isRoutePlanningMode()
				|| map.getMapLayers().getMapControlsLayer().getMapRouteInfoMenu().isVisible()
				|| addressTopBar.getVisibility() == View.VISIBLE) {
			updateVisibility(false);
			return;
		}

		LatLon loc = null;
		if (customLocation != null) {
			loc = customLocation;
		} else {
			Location l = map.getMapViewTrackingUtilities().getMyLocation();
			if (l != null) {
				loc = new LatLon(l.getLatitude(), l.getLongitude());
			}
		}
		Float heading = map.getMapViewTrackingUtilities().getHeading();

		MapMarker marker = markers.get(0);
		updateUI(loc, heading, marker, arrowImg, distText, okButton, addressText, true, customLocation != null);

		if (markers.size() > 1) {
			marker = markers.get(1);
			if (loc != null && customLocation == null) {
				for (int i = 1; i < markers.size(); i++) {
					MapMarker m = markers.get(i);
					m.dist = (int) (MapUtils.getDistance(m.getLatitude(), m.getLongitude(),
							loc.getLatitude(), loc.getLongitude()));
					if (m.dist < MIN_DIST_2ND_ROW_SHOW && marker.dist > m.dist) {
						marker = m;
					}
				}
			}
			updateUI(loc, heading, marker, arrowImg2nd, distText2nd, okButton2nd, addressText2nd, false, customLocation != null);
			updateVisibility(topBar2nd, true);
		} else {
			updateVisibility(topBar2nd, false);
		}

		updateVisibility(true);
	}

	private void updateUI(LatLon loc, Float heading, MapMarker marker, ImageView arrowImg,
						  TextView distText, ImageButton okButton, TextView addressText,
						  boolean firstLine, boolean customLocation) {
		float[] mes = new float[2];
		if (loc != null && marker.point != null) {
			Location.distanceBetween(marker.getLatitude(), marker.getLongitude(), loc.getLatitude(), loc.getLongitude(), mes);
		}

		boolean newImage = false;
		DirectionDrawable dd;
		if (!(arrowImg.getDrawable() instanceof DirectionDrawable)) {
			newImage = true;
			dd = new DirectionDrawable(map, arrowImg.getWidth(), arrowImg.getHeight());
		} else {
			dd = (DirectionDrawable) arrowImg.getDrawable();
		}
		dd.setImage(R.drawable.ic_arrow_marker_diretion, MapMarkerDialogHelper.getMapMarkerColorId(marker.colorIndex));
		if (heading != null && loc != null) {
			dd.setAngle(mes[1] - heading + 180 + screenOrientation);
		}
		if (newImage) {
			arrowImg.setImageDrawable(dd);
		}
		arrowImg.invalidate();

		int dist = (int) mes[0];
		String txt = null;
		if (loc != null) {
			txt = OsmAndFormatter.getFormattedDistance(dist, map.getMyApplication());
		} else {
			if ((firstLine && marker != this.marker) || (!firstLine && marker != this.marker2nd)) {
				txt = "—";
			}
		}
		if (txt != null) {
			distText.setText(txt);
		}
		updateVisibility(okButton, !customLocation && loc != null && dist < MIN_DIST_OK_VISIBLE);

		String descr;
		PointDescription pd = marker.getPointDescription(map);
		if (Algorithms.isEmpty(pd.getName())) {
			descr = pd.getTypeName();
		} else {
			descr = pd.getName();
		}
		if (!firstLine && !isLandscapeLayout()) {
			descr = "  •  " + descr;
		}

		addressText.setText(descr);

		if (firstLine) {
			this.marker = marker;
		} else {
			this.marker2nd = marker;
		}

	}

	public TextInfoWidget createMapMarkerControl(final MapActivity map, final boolean firstMarker) {
		DistanceToPointInfoControl ctrl = new DistanceToPointInfoControl(map, 0, 0) {
			private int cachedMarkerColorIndex = -1;
			private Boolean cachedNightMode = null;

			@Override
			public LatLon getPointToNavigate() {
				MapMarker marker = getMarker();
				if (marker != null) {
					return marker.point;
				}
				return null;
			}

			private MapMarker getMarker() {
				List<MapMarker> markers = helper.getSortedMapMarkers();
				if (firstMarker) {
					if (markers.size() > 0) {
						return markers.get(0);
					}
				} else {
					if (markers.size() > 1) {
						return markers.get(1);
					}
				}
				return null;
			}

			@Override
			public boolean updateInfo(DrawSettings drawSettings) {
				MapMarker marker = getMarker();
				if (marker == null) {
					setText(null, null);
					return false;
				}
				boolean res = super.updateInfo(drawSettings);

				if (marker.colorIndex != -1) {
					if (marker.colorIndex != cachedMarkerColorIndex
							|| cachedNightMode == null || cachedNightMode != isNight()) {
						setImageDrawable(map.getMyApplication().getIconsCache()
								.getIcon(isNight() ? R.drawable.widget_marker_night : R.drawable.widget_marker_day,
										R.drawable.widget_marker_triangle,
										MapMarkerDialogHelper.getMapMarkerColorId(marker.colorIndex)));
						cachedMarkerColorIndex = marker.colorIndex;
						cachedNightMode = isNight();
					}
				}
				return res;
			}

			@Override
			protected void click(OsmandMapTileView view) {
				showMarkerOnMap(firstMarker ? 0 : 1);
			}
		};
		ctrl.setAutoHide(false);
		return ctrl;
	}

	public boolean isLandscapeLayout() {
		return !portraitMode && !largeDevice;
	}
}
