<?php

namespace App\Nova;

use Illuminate\Http\Request;
use Laravel\Nova\Fields\BelongsTo;
use Laravel\Nova\Fields\Boolean;
use Laravel\Nova\Fields\HasMany;
use Laravel\Nova\Fields\ID;
use Laravel\Nova\Fields\Image;
use Laravel\Nova\Fields\Text;
use Laravel\Nova\Http\Requests\NovaRequest;

class MGallery extends Resource
{
    /**
     * The model the resource corresponds to.
     *
     * @var string
     */
    public static $model = \App\Models\MGallery::class;
    public static $displayInNavigation = false;
    public static $globallySearchable = false;


    /**
     * The single value that should be used to represent the resource when being displayed.
     *
     * @var string
     */
    public static $title = 'ArG_name';

    /**
     * The columns that should be searched.
     *
     * @var array
     */
    public static $search = [
        'ArG_name',
    ];

    public static function label()
    {
        return 'Снимки';
    }

    /**
     * Get the fields displayed by the resource.
     *
     * @param  \Illuminate\Http\Request  $request
     * @return array
     */
    public function fields(Request $request)
    {
        return [
            ID::make(__('ID'), 'ArG_id')->sortable(),
            Image::make('Снимка', 'ArG_file')->disk('public')->path('pub/gallery')->storeAs(function (Request $request) {
                return date('YmdGis') . '_' . $request->file('ArG_file')->getClientOriginalName();
            })->creationRules('required'),
            // Image::make('Снимка', 'ArG_file')->disk('public')->path('pub/gallery')->maxWidth(1600)->storeAs(function (Request $request) {
            //     return date('YmdGis') . '_' . $request->file('ArG_file')->getClientOriginalName();
            // })->rules('required', 'dimensions:max_width=1600'),
            Text::make('Име', 'ArG_name')->sortable(),
            BelongsTo::make('Новина', 'eq_news', MNews::class)->rules('required'),
            Boolean::make('Водеща снимка', 'ArG_pin'),
            // BelongsTo::make('Секция', 'eq_article', MArticle::class)->rules('required'),
            HasMany::make('Езикова версия', 'eq_lngGal', MGalleryLng::class),
        ];
    }

    /**
     * Get the cards available for the request.
     *
     * @param  \Illuminate\Http\Request  $request
     * @return array
     */
    public function cards(Request $request)
    {
        return [];
    }

    /**
     * Get the filters available for the resource.
     *
     * @param  \Illuminate\Http\Request  $request
     * @return array
     */
    public function filters(Request $request)
    {
        return [];
    }

    /**
     * Get the lenses available for the resource.
     *
     * @param  \Illuminate\Http\Request  $request
     * @return array
     */
    public function lenses(Request $request)
    {
        return [];
    }

    /**
     * Get the actions available for the resource.
     *
     * @param  \Illuminate\Http\Request  $request
     * @return array
     */
    public function actions(Request $request)
    {
        return [];
    }
}
