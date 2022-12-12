<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\SoftDeletes;

/**
 * @property int     $Cf_id
 * @property int     $S_Lng_id
 * @property int     $created_at
 * @property int     $updated_at
 * @property int     $deleted_at
 * @property string  $Cf_name
 * @property string  $Cf_value
 * @property boolean $St_id
 */
class MConfig extends Model
{
    use SoftDeletes;
    /**
     * The database table used by the model.
     *
     * @var string
     */
    protected $table = 'm_config';

    /**
     * The primary key for the model.
     *
     * @var string
     */
    protected $primaryKey = 'Cf_id';

    /**
     * Attributes that should be mass-assignable.
     *
     * @var array
     */
    protected $fillable = [
        'S_Lng_id', 'CfT_id', 'Cf_name', 'Cf_value', 'St_id', 'created_at', 'updated_at', 'deleted_at'
    ];

    /**
     * The attributes excluded from the model's JSON form.
     *
     * @var array
     */
    protected $hidden = [];

    /**
     * The attributes that should be casted to native types.
     *
     * @var array
     */
    protected $casts = [
        'Cf_id' => 'int', 'S_Lng_id' => 'int', 'CfT_id' => 'int', 'Cf_name' => 'string', 'Cf_value' => 'string', 'St_id' => 'boolean', 'created_at' => 'timestamp', 'updated_at' => 'timestamp', 'deleted_at' => 'timestamp'
    ];

    /**
     * The attributes that should be mutated to dates.
     *
     * @var array
     */
    protected $dates = [
        'created_at', 'updated_at', 'deleted_at'
    ];

    /**
     * Indicates if the model should be timestamped.
     *
     * @var boolean
     */
    public $timestamps = false;

    public static function boot()
    {
        parent::boot();


        static::creating(function ($article) {

            $article->created_at = now();
            $article->updated_at = now();
        });

        static::saving(function ($article) {

            $article->updated_at = now();
        });
    }

    // Scopes...

    // Functions ...

    // Relations ...

    public function eq_lng()
    {
        return $this->belongsTo(SLang::class, 'S_Lng_id');
    }
    public function eq_type()
    {
        return $this->belongsTo(MConfigType::class, 'CfT_id');
    }
    public function eq_lang()
    {
        return $this->hasMany(MConfigLng::class, 'Cf_id');
    }
}
