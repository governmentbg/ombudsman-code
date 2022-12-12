<?php

namespace App\Nova;

use Illuminate\Http\Request;
use Laravel\Nova\Fields\BelongsTo;
use Laravel\Nova\Fields\Boolean;
use Laravel\Nova\Fields\HasMany;
use Laravel\Nova\Fields\ID;
use Laravel\Nova\Fields\Text;
use Laravel\Nova\Http\Requests\NovaRequest;

class QTopic extends Resource
{
    /**
     * The model the resource corresponds to.
     *
     * @var string
     */
    public static $model = \App\Models\QTopic::class;
    public static $displayInNavigation = false;
    public static $globallySearchable = false;

    /**
     * The single value that should be used to represent the resource when being displayed.
     *
     * @var string
     */
    public static $title = 'Qt_name';

    /**
     * The columns that should be searched.
     *
     * @var array
     */
    public static $search = [
        'Qt_name',
    ];
    public static function label()
    {
        return 'Въпроси';
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
            ID::make(__('ID'), 'Qt_id')->sortable(),
            Text::make('Име', 'Qt_name')->sortable()->rules('required'),
            Text::make('Подредба', 'Qt_order')->creationRules('integer')->updateRules('integer')->sortable(),
            HasMany::make('Отговори', 'eq_answers', QAnswers::class),
            Boolean::make('Повече от един отговор', 'Qt_multi'),
            Boolean::make('Отговор свободен текст', 'Qt_freetext'),
            BelongsTo::make('Анкета', 'eq_quest', QQuest::class),
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
