<?php

namespace App\Nova;

use Illuminate\Http\Request;
use Laravel\Nova\Fields\BelongsTo;
use Laravel\Nova\Fields\Date;
use Laravel\Nova\Fields\File;
use Laravel\Nova\Fields\ID;
use Laravel\Nova\Fields\Text;
use Laravel\Nova\Fields\Textarea;
use Laravel\Nova\Http\Requests\NovaRequest;

class MFiles extends Resource
{
    /**
     * The model the resource corresponds to.
     *
     * @var string
     */
    public static $model = \App\Models\MFiles::class;
    public static $displayInNavigation = false;
    public static $globallySearchable = false;

    /**
     * The single value that should be used to represent the resource when being displayed.
     *
     * @var string
     */
    public static $title = 'ArF_name';
    public static function label()
    {
        return 'Файлове';
    }

    /**
     * The columns that should be searched.
     *
     * @var array
     */
    public static $search = [
        'ArF_name', 'ArF_file'
    ];

    /**
     * Get the fields displayed by the resource.
     *
     * @param  \Illuminate\Http\Request  $request
     * @return array
     */
    public function fields(Request $request)
    {
        return [
            ID::make(__('ID'), 'ArF_id')->sortable(),
            File::make('Файл', 'ArF_file')->disk('public')->path('pub/files')->storeAs(function (Request $request) {
                return date('YmdGis') . '_' . $request->file('ArF_file')->getClientOriginalName();
            })->storeSize('ArF_size')->creationRules('required'),



            Text::make('Име', 'ArF_name')->sortable()->rules('required'),
            Text::make('Размер', 'ArF_size')
                ->exceptOnForms()
                ->displayUsing(function ($value) {
                    return number_format($value / 1024, 2) . 'kb';
                }),
            Date::make('Дата', 'ArF_date')->sortable(),
            Textarea::make('Описание', 'ArF_desc')->sortable()->hideFromIndex(),

            BelongsTo::make('Секция', 'eq_article', MArticleLng::class)->rules('required'),
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
