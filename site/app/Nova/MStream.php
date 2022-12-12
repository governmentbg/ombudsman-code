<?php

namespace App\Nova;

use Laravel\Nova\Fields\DateTime;
use Illuminate\Http\Request;
use Laravel\Nova\Fields\BelongsTo;
use Laravel\Nova\Fields\ID;
use Laravel\Nova\Fields\Text;
use Illuminate\Support\Str;
use Laravel\Nova\Fields\Boolean;
use Laravel\Nova\Fields\Textarea;
use Laravel\Nova\Http\Requests\NovaRequest;

class MStream extends Resource
{
    /**
     * The model the resource corresponds to.
     *
     * @var string
     */
    public static $model = \App\Models\MStream::class;
    public static $displayInNavigation = false;
    public static $globallySearchable = false;
    public static function label()
    {
        return 'Видове стрийм';
    }

    /**
     * The single value that should be used to represent the resource when being displayed.
     *
     * @var string
     */
    public static $title = 'Str_url';

    /**
     * The columns that should be searched.
     *
     * @var array
     */
    public static $search = [
        'Str_url',
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
            ID::make(__('ID'), 'Str_id')->sortable(),
            Text::make('Адрес на стрийм', 'Str_url')->sortable(),
            Textarea::make('Embed code', 'Str_embed')->sortable()->displayUsing(function ($text) {
                return Str::limit($text, 60);
            }),
            DateTime::make('Начало', 'Str_start')->sortable(),
            DateTime::make('Край', 'End_start')->sortable(),



            Boolean::make('Видео на първа страница', 'Str_home'),

            BelongsTo::make('Събитие', 'eq_event', MEvent::class)->rules('required')->withoutTrashed(),

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
