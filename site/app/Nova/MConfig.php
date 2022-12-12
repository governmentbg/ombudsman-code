<?php

namespace App\Nova;

use Illuminate\Http\Request;
use Laravel\Nova\Fields\ID;
use Laravel\Nova\Fields\Text;
use Laravel\Nova\Http\Requests\NovaRequest;
use Illuminate\Support\Str;
use Laravel\Nova\Fields\BelongsTo;
use Laravel\Nova\Fields\Boolean;
use Laravel\Nova\Fields\HasMany;
use Laravel\Nova\Fields\Textarea;

class MConfig extends Resource
{
    /**
     * The model the resource corresponds to.
     *
     * @var string
     */
    public static $model = \App\Models\MConfig::class;
    // public static $globallySearchable = false;
    public static function label()
    {
        return 'Настройки';
    }

    /**
     * The single value that should be used to represent the resource when being displayed.
     *
     * @var string
     */
    public static $title = 'Cf_name';

    /**
     * The columns that should be searched.
     *
     * @var array
     */
    public static $search = [
        'Cf_name', 'Cf_value',
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
            ID::make(__('ID'), 'Cf_id')->sortable(),
            Text::make('Име', 'Cf_name')->sortable()->rules('required')->displayUsing(function ($text) {
                return Str::limit($text, 60);
            }),
            Textarea::make('Стойност', 'Cf_value')->sortable(),
            // Boolean::make('Активен запис', 'St_id')->sortable(),



            Boolean::make('Активен', 'St_id'),
            BelongsTo::make('Вид', 'eq_type', MConfigType::class)->withoutTrashed(),
            HasMany::make('Езиковa версия', 'eq_lang', MConfigLng::class),
            // BelongsTo::make('Език', 'eq_lng', SLang::class)->withoutTrashed(),

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
