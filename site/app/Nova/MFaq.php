<?php

namespace App\Nova;

use Illuminate\Http\Request;
use Laravel\Nova\Fields\Date;
use Laravel\Nova\Fields\HasMany;
use Laravel\Nova\Fields\ID;
use Laravel\Nova\Fields\Select;
use Laravel\Nova\Fields\Text;
use Illuminate\Support\Str;
use Laravel\Nova\Http\Requests\NovaRequest;

class MFaq extends Resource
{
    /**
     * The model the resource corresponds to.
     *
     * @var string
     */
    public static $model = \App\Models\MFaq::class;
    // public static $globallySearchable = false;

    /**
     * The single value that should be used to represent the resource when being displayed.
     *
     * @var string
     */
    public static $title = 'Fq_name';

    public static function label()
    {
        return 'Най-често задавани въпроси';
    }

    /**
     * The coluFqs that should be searched.
     *
     * @var array
     */
    public static $search = [
        'Fq_name',
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
            ID::make(__('ID'), 'Fq_id')->sortable()->hideWhenCreating(),

            Text::make('Заглавие', 'Fq_name')->sortable()->rules('required')->displayUsing(function ($text) {
                return Str::limit($text, 80);
            }),

            Select::make('Вид', 'Fq_type')->options([
                1 => 'Основна секция',
                2 => 'Подаване на жалба',
            ])->displayUsingLabels()->sortable()->rules('required'),

            Text::make('Подредба', 'Fq_order')->creationRules('integer')->updateRules('integer')->sortable(),


            Date::make('Дата', 'Fq_date')->sortable(),

            HasMany::make('Езиковa версия', 'eq_faq', MFaqLng::class),
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
