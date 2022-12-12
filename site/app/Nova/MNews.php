<?php

namespace App\Nova;

use Illuminate\Http\Request;
use Laravel\Nova\Fields\ID;
use Laravel\Nova\Http\Requests\NovaRequest;

use Laravel\Nova\Fields\BelongsTo;
use Laravel\Nova\Fields\Boolean;
use Laravel\Nova\Fields\Date;
use Laravel\Nova\Fields\HasMany;
use Laravel\Nova\Fields\Hidden;
use Laravel\Nova\Fields\Select;
use Laravel\Nova\Fields\Text;
use Laravel\Nova\Fields\Textarea;
use Illuminate\Support\Str;

class MNews extends Resource
{
    /**
     * The model the resource corresponds to.
     *
     * @var string
     */
    public static $model = \App\Models\MNews::class;
    // public static $globallySearchable = false;

    /**
     * The single value that should be used to represent the resource when being displayed.
     *
     * @var string
     */
    public static $title = 'Mn_name';

    public static function label()
    {
        return 'Новини / Актуално';
    }

    /**
     * The columns that should be searched.
     *
     * @var array
     */
    public static $search = [
        'Mn_name',
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

            ID::make(__('ID'), 'Mn_id')->sortable()->hideWhenCreating(),

            // Text::make('Заглавие', 'Mn_name')->sortable()->rules('required')->displayUsing(function ($text) {
            //     return Str::limit($text, 60);
            // }),
            Textarea::make('Заглавие', 'Mn_name')->sortable()->sortable()->rules('required')->showOnIndex(),

            Select::make('Вид', 'Mn_type')->options([
                1 => 'Новина',
                2 => 'Актуално събитие',
            ])->displayUsingLabels()->sortable()->rules('required'),
            Date::make('Дата', 'Mn_date')->rules('required')->sortable(),
            Text::make('Подредба', 'Mn_order')->sortable(),
            // Text::make('Подредба', 'Mn_order')->creationRules('integer')->updateRules('integer')->sortable(),
            Textarea::make('Вградено видео', 'Mn_embed_video')->sortable(),
            // Boolean::make('Активен запис', 'St_id'),
            Boolean::make('Водеща новина', 'Mn_pin'),

            HasMany::make('Езиковa версия', 'eq_lng', MNewsLng::class),
            HasMany::make('Снимки', 'eq_gallery', MGallery::class),


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
